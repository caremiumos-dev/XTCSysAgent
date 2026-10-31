package com.xtc.im.core.common.tlv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * IM 长连接的收流缓冲：socket 读上来的是字节流，一条消息可能被拆成几次到达、
 * 也可能一次到达好几条。这个类负责"攒够一条就切出来"。
 *
 * <p>切帧靠的是**首帧三连算**：先按 tag 段的续位（bit7）数出 tag 长度，
 * 再算出 length 段长度，最后解出 value 长度，三者相加就是首帧总长。
 * 算出总长后不再重复计算（三个字段都缓存着），只有 {@link #reset()} 才清空。</p>
 *
 * <p>原厂类名为单字母 {@code a}。</p>
 */
public class TLVFrameBuffer extends ByteArrayOutputStream {

    /** 调试开关：打开后把算出来的长度直接打到 stdout（原厂线上是关的）。 */
    private static boolean DEBUG = false;

    /** 首帧总长（tag + length + value）。 */
    private volatile int firstTotalSize = 0;

    /** 首帧 tag 段长度。 */
    private volatile int firstTagSize = 0;

    /** 首帧 length 段长度。 */
    private volatile int firstLengthSize = 0;

    private void print(String message) {
        if (DEBUG) {
            System.out.print(message);
        }
    }

    /**
     * 首帧还没算完就一路往下算：三个步骤各自会判"算过了没有"。
     */
    private void prepare() {
        if (this.count > 0) {
            parseFirstTagSize();
            parseFirstLengthSize();
            parseFirstTotalSize();
        }
    }

    /**
     * 数首帧 tag 段字节数。
     */
    private void parseFirstTagSize() {
        if (this.firstTagSize == 0) {
            this.firstTagSize = TLVCodec.countTagBytes(toByteArray());
            print("firstTagSize:" + this.firstTagSize);
        }
    }

    /**
     * 数首帧 length 段字节数（要跳过 tag 段）。
     */
    private void parseFirstLengthSize() {
        if (this.firstLengthSize != 0 || this.firstTagSize == 0) {
            return;
        }
        this.firstLengthSize = TLVCodec.countTagBytes(toByteArray(), this.firstTagSize);
        print("firstLengthSize:" + this.firstLengthSize);
    }

    /**
     * 解出 value 长度，加上前两段得到首帧总长。
     */
    private void parseFirstTotalSize() {
        if (this.firstTagSize <= 0 || this.firstLengthSize <= 0 || this.firstTotalSize != 0) {
            return;
        }
        byte[] lengthBytes = new byte[this.firstLengthSize];
        System.arraycopy(toByteArray(), this.firstTagSize, lengthBytes, 0, this.firstLengthSize);
        this.firstTotalSize = TLVCodec.lengthValue(lengthBytes) + this.firstTagSize + this.firstLengthSize;
        print("firstTotalSize:" + this.firstTotalSize);
    }

    /**
     * 缓冲区里是否已经攒够一整帧。
     */
    public synchronized boolean hasCompleteFrame() {
        if (this.count != 0) {
            prepare();
            if (this.firstTotalSize > 0 && this.count > 0 && this.firstTotalSize <= this.count) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取出一整帧。
     *
     * <p>刚好一帧就整块返回并清空；超过一帧就把剩下的字节留在缓冲里等下次；
     * 不够一帧只打一行 stderr 并返回 null——原厂用 {@code System.err} 而不是日志，
     * 这条输出在生产环境会进 logcat，保留以便排障。</p>
     */
    public synchronized byte[] takeFrame() {
        byte[] frame = null;
        if (this.firstTotalSize == this.count) {
            frame = toByteArray();
            reset();
        } else if (this.firstTotalSize < this.count) {
            byte[] remain = new byte[this.count - this.firstTotalSize];
            frame = new byte[this.firstTotalSize];
            System.arraycopy(toByteArray(), this.firstTotalSize, remain, 0, remain.length);
            System.arraycopy(toByteArray(), 0, frame, 0, frame.length);
            reset();
            write(remain, 0, remain.length);
        } else {
            System.err.println("firstTotalSize:" + this.firstTotalSize + ",count:" + this.count
                    + ",firstTotalSize must smaller than count!");
        }
        return frame;
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
    }

    /**
     * 清空缓冲时把三个长度缓存一起清掉，否则下一帧会沿用上一帧的长度。
     */
    @Override
    public synchronized void reset() {
        super.reset();
        this.firstTotalSize = 0;
        this.firstTagSize = 0;
        this.firstLengthSize = 0;
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) {
        super.write(bytes, offset, length);
    }
}
