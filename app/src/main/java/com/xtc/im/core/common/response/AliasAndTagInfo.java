package com.xtc.im.core.common.response;

/**
 * 一条 alias / tag 信息（不是协议实体，只是本地聚合出来的视图对象）。
 *
 * <p>构造参数只有 alias、pkgName、order、pageSize、syncKey 五个，
 * tag（{@code b}）与其它字段靠 setter 补——原厂如此，构造参数顺序保持原样。</p>
 *
 * <p>原厂类名为单字母 {@code a}，toString 里已经写着 {@code AliasAndTagInfo}。</p>
 */
public class AliasAndTagInfo {

    /** 别名。 */
    private String alias;

    /** 标签。 */
    private String tag;

    /** 所属包名。 */
    private String pkgName;

    /** 排序序号。 */
    private int order;

    /** 分页大小。 */
    private int pageSize;

    /** 同步位点。 */
    private long syncKey;

    public AliasAndTagInfo(String alias, String pkgName, int order, int pageSize, long syncKey) {
        this.alias = alias;
        this.pkgName = pkgName;
        this.order = order;
        this.pageSize = pageSize;
        this.syncKey = syncKey;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTag() {
        return this.tag;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public int getOrder() {
        return this.order;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public long getSyncKey() {
        return this.syncKey;
    }

    @Override
    public String toString() {
        return "AliasAndTagInfo{alias='" + this.alias + "', tag='" + this.tag + "', pkgName='"
                + this.pkgName + "', order=" + this.order + ", pageSize=" + this.pageSize
                + ", syncKey=" + this.syncKey + '}';
    }
}
