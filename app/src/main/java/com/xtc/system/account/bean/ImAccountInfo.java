package com.xtc.system.account.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * IM 账号信息：一个账号对应多个会话（dialog）。
 *
 * <p>字段名就是服务端协议字段，不能改。{@link #toList()} 的顺序是固定的 ——
 * 网络层按位置取，不是按名字，所以顺序错了会串号。</p>
 */
public class ImAccountInfo {

    private String accountId;
    private Long familyChatDialogId;
    private Long imAccountId;
    private Long locationDialogId;
    private Long noticeDialogId;
    private Long optFamilyDialogId;
    private Long readDialogId;
    private Long receiptPushDialogId;
    private Long receiveDialogId;
    private Long schoolGuardDialogId;
    private Long singlePushDialogId;
    private Long smsDialogId;

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getFamilyChatDialogId() {
        return this.familyChatDialogId;
    }

    public void setFamilyChatDialogId(Long familyChatDialogId) {
        this.familyChatDialogId = familyChatDialogId;
    }

    public Long getImAccountId() {
        return this.imAccountId;
    }

    public void setImAccountId(Long imAccountId) {
        this.imAccountId = imAccountId;
    }

    public Long getLocationDialogId() {
        return this.locationDialogId;
    }

    public void setLocationDialogId(Long locationDialogId) {
        this.locationDialogId = locationDialogId;
    }

    public Long getNoticeDialogId() {
        return this.noticeDialogId;
    }

    public void setNoticeDialogId(Long noticeDialogId) {
        this.noticeDialogId = noticeDialogId;
    }

    public Long getOptFamilyDialogId() {
        return this.optFamilyDialogId;
    }

    public void setOptFamilyDialogId(Long optFamilyDialogId) {
        this.optFamilyDialogId = optFamilyDialogId;
    }

    public Long getReadDialogId() {
        return this.readDialogId;
    }

    public void setReadDialogId(Long readDialogId) {
        this.readDialogId = readDialogId;
    }

    public Long getReceiptPushDialogId() {
        return this.receiptPushDialogId;
    }

    public void setReceiptPushDialogId(Long receiptPushDialogId) {
        this.receiptPushDialogId = receiptPushDialogId;
    }

    public Long getReceiveDialogId() {
        return this.receiveDialogId;
    }

    public void setReceiveDialogId(Long receiveDialogId) {
        this.receiveDialogId = receiveDialogId;
    }

    public Long getSchoolGuardDialogId() {
        return this.schoolGuardDialogId;
    }

    public void setSchoolGuardDialogId(Long schoolGuardDialogId) {
        this.schoolGuardDialogId = schoolGuardDialogId;
    }

    public Long getSinglePushDialogId() {
        return this.singlePushDialogId;
    }

    public void setSinglePushDialogId(Long singlePushDialogId) {
        this.singlePushDialogId = singlePushDialogId;
    }

    public Long getSmsDialogId() {
        return this.smsDialogId;
    }

    public void setSmsDialogId(Long smsDialogId) {
        this.smsDialogId = smsDialogId;
    }

    /**
     * 按协议约定的固定顺序展开成 List。
     *
     * <p>顺序：imAccountId、singlePush、receive、receiptPush、read、
     * familyChat、optFamily、location、schoolGuard、notice、sms。</p>
     */
    public List<Long> toList() {
        ArrayList<Long> dialogIds = new ArrayList<>();
        dialogIds.add(this.imAccountId);
        dialogIds.add(this.singlePushDialogId);
        dialogIds.add(this.receiveDialogId);
        dialogIds.add(this.receiptPushDialogId);
        dialogIds.add(this.readDialogId);
        dialogIds.add(this.familyChatDialogId);
        dialogIds.add(this.optFamilyDialogId);
        dialogIds.add(this.locationDialogId);
        dialogIds.add(this.schoolGuardDialogId);
        dialogIds.add(this.noticeDialogId);
        dialogIds.add(this.smsDialogId);
        return dialogIds;
    }

    @Override
    public String toString() {
        return "ImAccountInfo{accountId='" + this.accountId + '\''
                + ", imAccountId=" + this.imAccountId
                + ", singlePushDialogId=" + this.singlePushDialogId
                + ", receiveDialogId=" + this.receiveDialogId
                + ", receiptPushDialogId=" + this.receiptPushDialogId
                + ", readDialogId=" + this.readDialogId
                + ", familyChatDialogId=" + this.familyChatDialogId
                + ", optFamilyDialogId=" + this.optFamilyDialogId
                + ", locationDialogId=" + this.locationDialogId
                + ", schoolGuardDialogId=" + this.schoolGuardDialogId
                + ", noticeDialogId=" + this.noticeDialogId
                + ", smsDialogId=" + this.smsDialogId + '}';
    }
}
