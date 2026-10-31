package com.xtc.system.account;

import android.os.Environment;
import android.text.TextUtils;
import com.xtc.log.LogUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 注册信息在 SD 卡上的落地文件（{@code /sdcard/sync/}）。
 *
 * <p>两个文件：</p>
 * <ul>
 *   <li>{@code public_key.txt} —— 裸的 RSA 公钥字节，不是 JSON；</li>
 *   <li>{@code regist_info.txt} —— JSON，含 {@code registId} 与 {@code registToken}。</li>
 * </ul>
 *
 * <p>注意读文件时缓冲区是 10 字节且循环复用 —— {@link #getRegistId()} 里
 * <strong>没有</strong>用 {@code new String(buf, 0, len)}，末块会带上上一轮的残留字节；
 * 而 {@link #getRegistToken()} 用了长度参数。这个不对称是原厂就有的，
 * 因为 JSON 解析会忽略尾部垃圾，所以没暴露出来。保留原样。</p>
 */
class SDCardUtil {

    private static final String TAG = "SDCardUtil";

    /** 注册根目录。 */
    private static final String SD_CARD_DIR =
            Environment.getExternalStorageDirectory().getPath() + "/sync/";

    /** 公钥文件名。 */
    private static final String PUBLIC_KEY_FILE_NAME = "public_key.txt";

    /** 注册信息文件名。 */
    private static final String REGIST_INFO_FILE_NAME = "regist_info.txt";

    /** 读文件的缓冲区大小。原厂就是 10。 */
    private static final int BUFFER_SIZE = 10;

    private static final String KEY_REGIST_ID = "registId";
    private static final String KEY_REGIST_TOKEN = "registToken";

    SDCardUtil() {
    }

    /** 删掉 /sdcard/sync/ 下的某个文件。文件本来不存在也算成功。 */
    public static boolean deleteFile(String fileName) {
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir.getAbsolutePath() + "/" + fileName);
        if (!target.exists()) {
            return true;
        }
        if (target.delete()) {
            LogUtil.d(TAG, "delete " + fileName + " file success.");
            return true;
        }
        LogUtil.e(TAG, "delete" + fileName + " file fail.");
        return false;
    }

    public static boolean deletePublicKey() {
        return deleteFile(PUBLIC_KEY_FILE_NAME);
    }

    public static boolean deleteRegistInfo() {
        return deleteFile(REGIST_INFO_FILE_NAME);
    }

    /**
     * 读公钥。SD 卡不可用或文件不存在时返回 null。
     */
    public static byte[] getPublicKey() {
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            return null;
        }
        File target = new File(dir.getAbsolutePath() + "/" + PUBLIC_KEY_FILE_NAME);
        if (!target.exists()) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(target);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e);
            return null;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return outputStream.toByteArray();
    }

    /**
     * 读注册 id。读不到或 JSON 里没有该字段时返回 0。
     *
     * <p>拼接时故意没传长度参数 —— 保留原厂的字节残留行为，见类注释。</p>
     */
    public static long getRegistId() {
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            return 0L;
        }
        File target = new File(dir.getAbsolutePath() + "/" + REGIST_INFO_FILE_NAME);
        if (!target.exists()) {
            return 0L;
        }
        StringBuilder content = new StringBuilder();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(target);
            byte[] buffer = new byte[BUFFER_SIZE];
            while (inputStream.read(buffer) != -1) {
                content.append(new String(buffer));
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e);
            return 0L;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return 0L;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return getLong(content.toString(), KEY_REGIST_ID);
    }

    /**
     * 读注册 token。读不到时返回 null。
     *
     * <p>这里传了长度参数，所以没有 {@link #getRegistId()} 的残留问题。</p>
     */
    public static String getRegistToken() {
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            return null;
        }
        File target = new File(dir.getAbsolutePath() + "/" + REGIST_INFO_FILE_NAME);
        if (!target.exists()) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(target);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, read));
            }
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e);
            return null;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }
        return getString(content.toString(), KEY_REGIST_TOKEN);
    }

    private static long getLong(String json, String key) {
        try {
            return new JSONObject(json).getLong(key);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
            return 0L;
        }
    }

    private static String getString(String json, String key) {
        try {
            return new JSONObject(json).getString(key);
        } catch (JSONException e) {
            LogUtil.e(TAG, e);
            return null;
        }
    }

    public static boolean hasPublicKey() {
        return getPublicKey() != null;
    }

    /** 已注册：id 非 0 且 token 非空。 */
    public static boolean isRegisted() {
        long registId = getRegistId();
        String registToken = getRegistToken();
        if (registId == 0 || TextUtils.isEmpty(registToken)) {
            return false;
        }
        LogUtil.i(TAG, "regist_info had existed,registId=" + registId
                + ",registToken=" + registToken);
        return true;
    }

    /** 已注册且 id/token 都与给定的匹配。 */
    private static boolean isRegisted(long registId, String registToken) {
        long savedId = getRegistId();
        String savedToken = getRegistToken();
        if (savedId == 0 || TextUtils.isEmpty(savedToken)
                || savedId != registId || !savedToken.equals(registToken)) {
            return false;
        }
        LogUtil.i(TAG, "regist_info had existed,registId = " + registId
                + " ,registToken = " + registToken);
        return true;
    }

    /** 覆盖写公钥。先删旧文件再写。 */
    public static boolean savePublicKey(byte[] publicKey) {
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(new StringBuilder().append(dir.getAbsolutePath())
                .append("/").append(PUBLIC_KEY_FILE_NAME).toString());
        if (target.exists()) {
            if (target.delete()) {
                LogUtil.i(TAG, "delete " + target.getName() + " success.");
            } else {
                LogUtil.e(TAG, "delete " + target.getName() + " fail.");
            }
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(target);
            outputStream.write(publicKey);
            LogUtil.i(TAG, "save data to file success,data = " + Arrays.toString(publicKey));
            return true;
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e);
            return false;
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }
    }

    /**
     * 写注册信息。
     *
     * <p>三种情况：</p>
     * <ul>
     *   <li>文件不存在 —— 直接写；</li>
     *   <li>文件存在且内容一致 —— 什么都不做，返回 false；</li>
     *   <li>文件存在但内容不一致 —— 删掉重写。</li>
     * </ul>
     */
    public static boolean saveRegistInfo(long registId, String registToken) throws Throwable {
        boolean saved = false;
        File dir = new File(SD_CARD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(new StringBuilder().append(dir.getAbsolutePath())
                .append("/").append(REGIST_INFO_FILE_NAME).toString());
        if (!target.exists()) {
            JSONObject json = new JSONObject();
            try {
                json.put(KEY_REGIST_ID, registId);
                json.put(KEY_REGIST_TOKEN, registToken);
            } catch (JSONException e) {
                LogUtil.e(TAG, e);
            }
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(target);
                outputStream.write(json.toString().getBytes());
                LogUtil.i(TAG, "save data to file success,data:" + json.toString());
                saved = true;
            } catch (FileNotFoundException e) {
                LogUtil.e(TAG, e);
            } catch (IOException e) {
                LogUtil.e(TAG, e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        LogUtil.e(TAG, e);
                    }
                }
            }
        } else if (!isRegisted(registId, registToken)) {
            if (target.delete()) {
                LogUtil.i(TAG, "delete " + target.getName() + " success.");
            } else {
                LogUtil.e(TAG, "delete " + target.getName() + " fail.");
            }
            JSONObject json = new JSONObject();
            json.put(KEY_REGIST_ID, registId);
            json.put(KEY_REGIST_TOKEN, registToken);
            FileOutputStream outputStream = new FileOutputStream(target);
            outputStream.write(json.toString().getBytes());
            LogUtil.i(TAG, "save data to file success,data:" + json.toString());
            saved = true;
            outputStream.close();
        }
        return saved;
    }
}
