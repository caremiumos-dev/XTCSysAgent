package com.xtc.dns.storage.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * HttpDns 缓存表 {@code domain_model} 的一行（原厂 {@code com.xtc.dns.storage.db.b}）。
 *
 * <p>一个域名可能有多条 ip，每条一行，靠 {@code priority} 排优先级：
 * 连接失败就被 {@code decreasePriority} 减一，值越小越靠前（查询按 priority 降序排）。</p>
 *
 * <p>列名与索引名都是**数据库里已有的物理名**，改了等于换库结构，
 * 老用户的缓存会直接失效，所以逐字保持原值。</p>
 */
@DatabaseTable(tableName = "domain_model")
public class DomainModel {

    /** 自增主键。 */
    @DatabaseField(columnName = "id", generatedId = true)
    private long id;

    /** 域名。 */
    @DatabaseField(columnName = "domain", uniqueIndexName = "_idx_domain_ip")
    private String domain = "";

    /** ip。 */
    @DatabaseField(columnName = "ip", indexName = "_id_ip", uniqueIndexName = "_idx_domain_ip")
    private String ip = "";

    /** ip 级 ttl（秒）。 */
    @DatabaseField(columnName = "ip_ttl")
    private long ipTtl = 0;

    /** 优先级（越小越优先，降权时减一）。 */
    @DatabaseField(columnName = "priority")
    private int priority = 0;

    /** 来源标记。 */
    @DatabaseField(columnName = "sp")
    private String sp = "";

    /** 域名级 ttl（秒）。 */
    @DatabaseField(columnName = "domain_ttl")
    private long domainTtl;

    /** 记录创建时间（毫秒时间戳）。 */
    @DatabaseField(columnName = "create_time")
    private long createTime;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getIpTtl() {
        return this.ipTtl;
    }

    public void setIpTtl(long ipTtl) {
        this.ipTtl = ipTtl;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getSp() {
        return this.sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public long getDomainTtl() {
        return this.domainTtl;
    }

    public void setDomainTtl(long domainTtl) {
        this.domainTtl = domainTtl;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    /** 日志用，字段顺序与原厂逐字一致。 */
    @Override
    public String toString() {
        return "DomainModel{id=" + this.id + ", domain='" + this.domain + "', ip='" + this.ip
                + "', ipTTL='" + this.ipTtl + "', priority=" + this.priority
                + ", sp='" + this.sp + "', domainTTL=" + this.domainTtl
                + ", createTime=" + this.createTime + '}';
    }
}
