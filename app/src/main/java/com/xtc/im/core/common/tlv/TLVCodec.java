package com.xtc.im.core.common.tlv;

import com.xtc.log.LogUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TLV 字节流解析器。
 *
 * <p>编码格式（与原厂一致）：tag 段用 bit7 做续位，bit6 是帧类型，bit5 是数据类型
 * （32 = 容器，值里还嵌套一串 TLV），低位是 tag 值；length 段同样用 bit7 续位；
 * 之后是 value 段。</p>
 *
 * <p>原厂类名为单字母 {@code d}。</p>
 */
public class TLVCodec {

    /** 调试开关，打开后解析过程的长度信息打到 stdout。 */
    private static boolean DEBUG = false;

    /**
     * 从 {@code offset} 开始数 tag 段字节数（遇到 bit7 为 0 的字节结束）。
     *
     * @return 数到结尾都没结束（畸形报文）时返回 0
     */
    public static int countTagBytes(byte[] data, int offset) {
        int count = 0;
        while (offset < data.length) {
            count++;
            if ((data[offset] & 128) == 0) {
                return count;
            }
            offset++;
        }
        return 0;
    }

    /**
     * 解析一条 TLV。
     */
    public static TLVDecodeResult decode(byte[] data) throws Throwable {
        return decodeSingle(data);
    }

    private static void print(String message) {
        if (DEBUG) {
            System.out.print(message);
        }
    }

    /**
     * 数一段字节里包含多少个 TLV 节点（容器节点会递归数它内部）。
     */
    public static int countFrames(byte[] data) {
        int tagSize = countTagBytes(data);
        byte[] tagBytes = new byte[tagSize];
        System.arraycopy(data, 0, tagBytes, 0, tagSize);
        int lengthSize = countTagBytes(data, tagSize);
        byte[] lengthBytes = new byte[lengthSize];
        System.arraycopy(data, tagSize, lengthBytes, 0, lengthSize);
        int valueSize = lengthValue(lengthBytes);
        byte[] valueBytes = new byte[valueSize];
        System.arraycopy(data, tagSize + lengthSize, valueBytes, 0, valueSize);
        int dataType = dataType(tagBytes);
        // 原厂在这里构造过一个 TLVDecodeResult 却从未使用（setter 没有副作用），
        // 为保持调用序列原样保留。
        TLVDecodeResult unused = new TLVDecodeResult();
        unused.setFrameType(frameType(tagBytes));
        unused.setDataType(dataType);
        unused.setTagValue(tagValue(tagBytes));
        unused.setLength(lengthValue(lengthBytes));
        int frames = dataType == 32 ? countFrames(valueBytes) + 1 : 1;
        int total = data.length;
        int head = tagSize + lengthSize + valueSize;
        if (total <= head) {
            return frames;
        }
        byte[] rest = new byte[total - head];
        System.arraycopy(data, head, rest, 0, total - head);
        return frames + countFrames(rest);
    }

    /**
     * 从数组头部数 tag 段字节数。
     */
    public static int countTagBytes(byte[] data) {
        int count = 0;
        for (byte value : data) {
            count++;
            if ((value & 128) == 0) {
                return count;
            }
        }
        return 0;
    }

    /**
     * 取帧类型（bit6）。
     */
    public static int frameType(byte[] tagBytes) {
        return tagBytes[0] & 64;
    }

    /**
     * 取数据类型（bit5），32 表示容器。
     */
    public static int dataType(byte[] tagBytes) {
        return tagBytes[0] & 32;
    }

    /**
     * 取 tag 值：单字节直接用低位；多字节走续位解析。
     */
    public static int tagValue(byte[] tagBytes) {
        return (tagBytes[0] & 128) != 128 ? tagBytes[0] & 31 : parseMultiByteTag(tagBytes);
    }

    /**
     * 取 length 段表示的长度。
     */
    public static int lengthValue(byte[] lengthBytes) {
        if ((lengthBytes[0] & 128) != 128) {
            return (int) TLVNumberUtils.toLong(lengthBytes);
        }
        int value = 0 | (lengthBytes[0] & 127);
        for (int i = 1; i < lengthBytes.length; i++) {
            value |= (lengthBytes[i] & 127) << (i * 7);
        }
        return value;
    }

    /**
     * 解一条 TLV。
     *
     * <p>容器节点（数据类型 32）的值会再递归解成一串子节点；如果声明长度和实际
     * 剩余字节对不上，只打一条错误日志然后继续按声明长度解——畸形报文不允许抛异常。</p>
     */
    private static TLVDecodeResult decodeSingle(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return null;
        }
        int tagSize = countTagBytes(data);
        byte[] tagBytes = new byte[tagSize];
        System.arraycopy(data, 0, tagBytes, 0, tagSize);
        int lengthSize = countTagBytes(data, tagSize);
        byte[] lengthBytes = new byte[lengthSize];
        System.arraycopy(data, tagSize, lengthBytes, 0, lengthSize);
        int valueSize = lengthValue(lengthBytes);
        byte[] valueBytes = new byte[valueSize];
        System.arraycopy(data, tagSize + lengthSize, valueBytes, 0, valueSize);
        Object value = valueBytes;
        if (dataType(tagBytes) == 32) {
            if ((data.length - tagSize) - lengthSize != valueSize) {
                LogUtil.e("tlv data may happen error because of data size is incorrect");
            }
            value = decodeChildren(valueBytes);
        }
        TLVDecodeResult result = new TLVDecodeResult();
        result.setFrameType(frameType(tagBytes));
        result.setDataType(dataType(tagBytes));
        result.setTagValue(tagValue(tagBytes));
        result.setLength(lengthValue(lengthBytes));
        result.setValue(value);
        return result;
    }

    /**
     * 把容器节点的值拆成一串子节点。
     */
    private static List<TLVDecodeResult> decodeChildren(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return null;
        }
        TLVFrameBuffer buffer = new TLVFrameBuffer();
        buffer.write(data);
        List<TLVDecodeResult> children = new ArrayList<TLVDecodeResult>();
        while (buffer.hasCompleteFrame()) {
            children.add(decodeSingle(buffer.takeFrame()));
        }
        return children;
    }

    /**
     * 从高位往低位拼多字节长度。
     *
     * <p>本类没有调用点（原厂就是这样留着），保留是为了与
     * {@code lengthValue} 的另一种字节序对应，删掉会丢信息。</p>
     */
    private static int parseLengthDescending(byte[] lengthBytes) {
        int value = 0;
        for (int i = 1; i < lengthBytes.length; i++) {
            value |= (lengthBytes[i] & 127) << (((lengthBytes.length - i) - 1) * 7);
        }
        return value;
    }

    /**
     * 从低位往高位拼多字节 tag。
     */
    private static int parseMultiByteTag(byte[] tagBytes) {
        int value = 0;
        for (int i = 1; i < tagBytes.length; i++) {
            value |= (tagBytes[i] & 127) << ((i - 1) * 7);
        }
        return value;
    }
}
