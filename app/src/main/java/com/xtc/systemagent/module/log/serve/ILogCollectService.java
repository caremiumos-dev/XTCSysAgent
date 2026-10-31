package com.xtc.systemagent.module.log.serve;

import com.xtc.systemagent.net.bean.LogStrategy;
import java.util.List;

/**
 * 日志采集总入口。
 */
public interface ILogCollectService {

    /** 采集成功。 */
    int COLLECT_STATUS_SUCCESS = 1;
    /** 采集失败。 */
    int COLLECT_STATUS_FAIL = 2;

    /** 用服务端下发的配置初始化采集策略。 */
    void initLogCollectStrategy(String config);

    /** 保存策略列表。 */
    void saveLogStrategies(List<LogStrategy> strategies);

    /** 上传日志。 */
    void startUploadLog(List<LogStrategy> strategies);

    /** 上传指定路径的文件。 */
    void uploadFile(String filePath);
}
