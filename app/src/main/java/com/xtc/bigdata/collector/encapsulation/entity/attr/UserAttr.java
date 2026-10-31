package com.xtc.bigdata.collector.encapsulation.entity.attr;

import android.content.ContentValues;

import com.xtc.bigdata.collector.encapsulation.interfaces.IAttr;
import com.xtc.bigdata.common.db.constant.Columns;

/**
 * 用户属性表。
 *
 * <p>字段名就是 Gson 存进 SharedPreferences 的键名（{@code userInfo}），
 * 改一个字母就会把已登录用户读成未登录。</p>
 */
public class UserAttr implements IAttr {

    /** 手机号。 */
    private String phoneNum;

    /** 用户扩展。 */
    private String userExtend;

    /** 用户 id。 */
    private String userId;

    /** 用户名。 */
    private String userName;

    @Override
    public IAttr clone() {
        UserAttr userAttr = new UserAttr();
        userAttr.phoneNum = this.phoneNum;
        userAttr.userExtend = this.userExtend;
        userAttr.userId = this.userId;
        userAttr.userName = this.userName;
        return userAttr;
    }

    /** @return 手机号 */
    public String getPhoneNum() {
        return this.phoneNum;
    }

    /** @return 用户扩展 */
    public String getUserExtend() {
        return this.userExtend;
    }

    /** @return 用户 id */
    public String getUserId() {
        return this.userId;
    }

    /** @return 用户名 */
    public String getUserName() {
        return this.userName;
    }

    @Override
    public void insert(ContentValues contentValues) {
        contentValues.put(Columns.COLUMN_UA_USERID, this.userId);
        contentValues.put(Columns.COLUMN_UA_USERNAME, this.userName);
        contentValues.put(Columns.COLUMN_UA_PHONENUM, this.phoneNum);
        contentValues.put(Columns.COLUMN_UA_USEREXTEND, this.userExtend);
    }

    /**
     * 设置手机号。
     *
     * @param phoneNum 手机号
     * @return this
     */
    public UserAttr setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
        return this;
    }

    /**
     * 设置用户扩展。
     *
     * @param userExtend 扩展
     * @return this
     */
    public UserAttr setUserExtend(String userExtend) {
        this.userExtend = userExtend;
        return this;
    }

    /**
     * 设置用户 id。
     *
     * @param userId 用户 id
     * @return this
     */
    public UserAttr setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 设置用户名。
     *
     * @param userName 用户名
     * @return this
     */
    public UserAttr setUserName(String userName) {
        this.userName = userName;
        return this;
    }
}
