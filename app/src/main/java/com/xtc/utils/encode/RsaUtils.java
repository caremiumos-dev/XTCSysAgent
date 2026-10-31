package com.xtc.utils.encode;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * RSA 加解密与验签工具。
 *
 * <p>原厂类名被混淆成 {@code i}。密钥一律是 Base64 编码的 X.509（公钥）/ PKCS#8（私钥）串，
 * 加解密走 {@link #TRANSFORMATION}，密文再用本包的 Base64 编一次；验签用 SHA1WithRSA。
 * 所有对外方法都把异常吞掉并返回 null / false，调用方（HttpHelper.getEebbkKey 等）
 * 依赖 "拿不到密钥就当没配" 的行为。</p>
 */
public class RsaUtils {

    /** 原厂内置的 RSA 公钥（Base64 X.509），用于和服务端交换 eebbkKey。 */
    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDfQz49r6aXpY49YPr3a9n1PHUy/bYzufv/Ag3Cdv1GLtugWy3qYBE6uj2CrRuBGgaWlmTtWpw9EmQtnen4xsKjxw8eqqNqud+pAUEG4k2YLEBe79MOuslld6R6vT+a9kvpY60rGO7/Pm+x8fVyCZvIQouzbf+T2b/GDTU0XNJR0wIDAQAB";

    /** 加解密变换串。 */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /** 明文/密文转字符串用的字符集。 */
    private static final String CHARSET = "UTF-8";

    /**
     * 用公钥加密（对应原厂 {@code i.b}，HttpHelper.getEebbkKey 用的就是它）。
     *
     * @param content          明文
     * @param publicKeyBase64  Base64 的 X.509 公钥
     * @return Base64 密文；失败返回 null
     */
    public static String encryptByPublicKey(String content, String publicKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKey(publicKeyBase64));
            return Base64Utils.encode(cipher.doFinal(content.getBytes(CHARSET)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 用私钥加密（对应原厂 {@code i.a}，线上没有调用方）。
     *
     * @param content           明文
     * @param privateKeyBase64  Base64 的 PKCS#8 私钥
     * @return Base64 密文；失败返回 null
     */
    public static String encryptByPrivateKey(String content, String privateKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, loadPrivateKey(privateKeyBase64));
            return Base64Utils.encode(cipher.doFinal(content.getBytes(CHARSET)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 用私钥解密（对应原厂 {@code i.c}）。
     *
     * @param content           Base64 密文
     * @param privateKeyBase64  Base64 的 PKCS#8 私钥
     * @return 明文；失败返回 null
     */
    public static String decryptByPrivateKey(String content, String privateKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, loadPrivateKey(privateKeyBase64));
            return new String(cipher.doFinal(Base64Utils.decode(content)), CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 用公钥解密（对应原厂 {@code i.d}）。
     *
     * @param content          Base64 密文
     * @param publicKeyBase64  Base64 的 X.509 公钥
     * @return 明文；失败返回 null
     */
    public static String decryptByPublicKey(String content, String publicKeyBase64) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, loadPublicKey(publicKeyBase64));
            return new String(cipher.doFinal(Base64Utils.decode(content)), CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 验签：SHA1WithRSA。
     *
     * <p>这里的 Base64 解码用的是 android.util.Base64（flag 为 DEFAULT），
     * 和本包 Base64Utils 的宽容策略不同，原厂就是这样混用的，不能统一。</p>
     *
     * @param content         原文
     * @param signBase64      签名（Base64）
     * @param publicKeyBase64 Base64 的 X.509 公钥
     * @return 验签是否通过；任何异常都返回 false
     */
    public static boolean verifySignature(String content, String signBase64, String publicKeyBase64) {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.DEFAULT)));
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(publicKey);
            // 原文没有指定字符集，走平台默认（原厂行为）
            signature.update(content.getBytes());
            return signature.verify(Base64.decode(signBase64, Base64.DEFAULT));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 由 Base64 的 X.509 串加载公钥。
     */
    private static PublicKey loadPublicKey(String publicKeyBase64) throws Exception {
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64Utils.decode(publicKeyBase64)));
    }

    /**
     * 由 Base64 的 PKCS#8 串加载私钥。
     */
    private static PrivateKey loadPrivateKey(String privateKeyBase64) throws Exception {
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64Utils.decode(privateKeyBase64)));
    }

    /**
     * 原厂留下的自测入口：用两把测试密钥各跑一次加解密，结果丢弃。
     *
     * <p>线上没有任何调用方，只是为了保留 "参数、调用顺序、异常向上抛" 的原样，
     * 因此不要删。</p>
     */
    public static void selfTest(String[] args) throws Exception {
        // 原厂第一句是 b("heal", 测试公钥)：用公钥加密
        encryptByPublicKey("heal", "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAOD5Y19YslL7R9Srz33ra/2+RVGinKz6r1bGS8Rcr7a2/zPLvkGpscMdrpFiXFIUXLTnFLPjGNNpvkqfJKKCuHMCAwEAAQ==");
        // 原厂第二句是 c(测试密文, 测试私钥)：用私钥解密
        decryptByPrivateKey("X521AF2AvjRd4FCBPBKSzF+zWpgknWjKygyQFgHgY2JSpc21rTJh3uc39kV4BXvivc3X6QNGnjXPnIgH1f6epg==", "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAovOBRlkhAd7M3RBJzVlvwvxyxhMjksJmCNwdWGQF4n2RgLEKzc3b9wuOs9Q3BOEFjimDiXnd9N2iP9uYP7a/VQIDAQABAkB64BoMfSs5qNNco2qzkYyIQSsfF9GMWlDsv2bVf188oPU5UQBkDPT239NLe6wiqFCB3X5+jqqxcBlWkVDeOSYlAiEA0+WvYfhXw/FiyCNxxJ6hblhRfQ/CrRKMz89BlC3t+FsCIQDE3eBeL90/OLS8X3qAIA3wChON4VmGzVznRWk0ssY2DwIhAITGxfET1pr3ZLiYTS+xXuJwAQ/mkkw09Xs6GZOqfBVFAiAmhmj25ZT9X0J3LpQRaLRxifdDp5rWd2+7zmiFKIsDXwIhAI+t4kFXZIGr0HE8DmkKjhTmMiVNsPOYswNumeh33XT5");
    }
}
