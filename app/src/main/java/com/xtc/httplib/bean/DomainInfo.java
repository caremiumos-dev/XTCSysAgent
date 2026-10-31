package com.xtc.httplib.bean;

import java.io.Serializable;

/**
 * 服务端下发的一份完整域名配置（落盘在配置文件里，每行一个 JSON）。
 *
 * <p>{@code domainId} 是环境 id：{@code "1"} 表示正式环境。它同时参与
 * {@link #equals(Object)}，所以两个配置对象是否"同一个环境"只看 domainId。</p>
 */
public class DomainInfo implements Serializable {

    /**
     * 刻意不声明 {@code serialVersionUID}：原厂也没有声明，靠字段/方法签名算出同一个
     * 默认值。这里补一个 1L 会让序列化 id 与原厂不一致，历史落盘数据将无法反序列化。
     */

    /** 业务域名集合。 */
    private AppDomain appDomain;
    /** 环境 id，{@code "1"} 为正式环境。 */
    private String domainId;
    /** IM 长连接域名集合。 */
    private ImDomain imDomain;
    /** 配置名称（例如"正式环境"）。 */
    private String name;

    /**
     * 只按 {@code domainId} 判等。
     *
     * <p>注意空值语义：两边都为空也算相等（原厂如此，用于"未下发配置"的比对）。
     * 没有重写 {@code hashCode}，所以本类不适合直接放进 HashSet/HashMap。</p>
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DomainInfo other = (DomainInfo) obj;
        if (this.domainId != null) {
            return this.domainId.equals(other.domainId);
        }
        return other.domainId == null;
    }

    public AppDomain getAppDomain() {
        return this.appDomain;
    }

    public String getDomainId() {
        return this.domainId;
    }

    public ImDomain getImDomain() {
        return this.imDomain;
    }

    public String getName() {
        return this.name;
    }

    public void setAppDomain(AppDomain appDomain) {
        this.appDomain = appDomain;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setImDomain(ImDomain imDomain) {
        this.imDomain = imDomain;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 拼接格式被日志消费，保持原样。
     *
     * <p>注意 {@code name='...'} 与 {@code domainId='...'} 之间**没有逗号**——
     * 这是原厂就有的笔误，改掉会让日志格式与线上对不上，所以保留。</p>
     */
    @Override
    public String toString() {
        return "DomainInfo{name='" + this.name + "'domainId='" + this.domainId
                + "', appDomain=" + this.appDomain
                + ", imDomain=" + this.imDomain + '}';
    }
}
