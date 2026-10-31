package com.xtc.systemagent.module.sys.bean;

import java.util.List;

/**
 * "删除手表文件" IMM 指令体。
 */
public class DeleteImContent {

    private int id;
    private String bindnumber;
    private List<String> files;

    /**
     * 把文件列表拼成逗号分隔的字符串，供 toString 使用。
     */
    private String getFilesString() {
        if (this.files == null || this.files.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String file : this.files) {
            builder.append(file);
            builder.append(",");
        }
        return builder.toString();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBindnumber() {
        return this.bindnumber;
    }

    public void setBindnumber(String bindnumber) {
        this.bindnumber = bindnumber;
    }

    public List<String> getFiles() {
        return this.files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "DeleteImContent{id=" + this.id
                + ", bindnumber=" + this.bindnumber
                + ", files:" + getFilesString() + '}';
    }
}
