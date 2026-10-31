package com.xtc.qiniu.bean;

import java.util.List;

/**
 * 批量取下载 token 的请求参数。
 *
 * <p>图片类业务可以通过 {@code longEdge/shortEdge/quality/format} 让服务端
 * 直接签发带缩略参数的链接。</p>
 */
public class NetDownloadTokenParam {

    /** 业务类型。 */
    private int fileType;

    /** 要下载的 key 列表。 */
    private List<String> keys;

    /** 长边像素。 */
    private Integer longEdge;

    /** 短边像素。 */
    private Integer shortEdge;

    /** 图片质量。 */
    private Integer quality;

    /** 目标格式。 */
    private String format;

    public int getFileType() {
        return this.fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public List<String> getKeys() {
        return this.keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public Integer getLongEdge() {
        return this.longEdge;
    }

    public void setLongEdge(Integer longEdge) {
        this.longEdge = longEdge;
    }

    public Integer getShortEdge() {
        return this.shortEdge;
    }

    public void setShortEdge(Integer shortEdge) {
        this.shortEdge = shortEdge;
    }

    public Integer getQuality() {
        return this.quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "NetDownloadTokenParam{fileType=" + this.fileType
                + ", keys=" + this.keys
                + ", longEdge=" + this.longEdge
                + ", shortEdge=" + this.shortEdge
                + ", quality=" + this.quality
                + ", format='" + this.format + "'}";
    }
}
