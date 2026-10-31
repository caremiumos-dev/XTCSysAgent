package com.xtc.systemagent.module.watchDrop;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.bigData.BigdataUtil;
import com.xtc.utils.common.CollectionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 自由落体（手表跌落）检测。
 *
 * <p>用的是 {@code TYPE_SIGNIFICANT_MOTION}(6) 传感器：它只在检测到"显著运动"
 * 时上报一次，值是 9.0 表示一次跌落。注册时的采样延迟是
 * {@code SENSOR_DELAY_FASTEST}(0) 之上的 3 —— 原厂传的就是 3。</p>
 */
public class WatchDrop {

    private static final String TAG = "Log_WatchDrop";

    /** 显著运动传感器，用于检测跌落。 */
    private static final int SENSOR_TYPE_SIGNIFICANT_MOTION = 6;

    /** 注册传感器的采样延迟参数，原厂写死为 3。 */
    private static final int SENSOR_DELAY = 3;

    /** 传感器上报的"跌落"特征值。 */
    private static final float DROP_VALUE = 9.0f;

    private Context context;
    private final SensorManager sensorManager;
    private List<Sensor> sensors = new ArrayList<>();

    private final SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[0];
            LogUtil.d(TAG, "onSensorChanged---value = " + value);
            if (value == DROP_VALUE) {
                LogUtil.w(TAG, "当前是跌落=====================");
                BigdataUtil.watchDropEvent(WatchDrop.this.context);
            }
        }
    };

    private final Sensor sensor = findSensor(SENSOR_TYPE_SIGNIFICANT_MOTION);

    public WatchDrop(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    /** 按类型找传感器；缓存列表为空时才重新查一次。 */
    private Sensor findSensor(int type) {
        if (CollectionUtils.isEmpty(this.sensors)) {
            this.sensors = this.sensorManager.getSensorList(Sensor.TYPE_ALL);
        }
        for (Sensor sensor : this.sensors) {
            if (sensor.getType() == type) {
                return sensor;
            }
        }
        return null;
    }

    /** 注册跌落监听。 */
    public void registerDropSensor() {
        this.sensorManager.registerListener(this.listener, this.sensor, SENSOR_DELAY);
    }

    /** 注销跌落监听。 */
    public void unRegisterDropSensor() {
        this.sensorManager.unregisterListener(this.listener);
    }
}
