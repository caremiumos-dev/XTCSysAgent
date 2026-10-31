package com.xtc.systemagent.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.xtc.log.LogUtil;
import com.xtc.systemagent.R;
import com.xtc.systemagent.module.log.serve.impl.ModemLogServeImpl;
import com.xtc.systemagent.net.bean.ExtraMes;
import com.xtc.utils.date.DateUtils;
import com.xtc.utils.encode.Md5Utils;
import com.xtc.utils.system.SystemConstants;
import com.xtc.utils.system.SystemPropertiesUtils;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地触发日志抓取的暗码页面。
 *
 * <p>入口是 {@code SecretCodeReceiver}：手表上输入暗码后弹出本页面，
 * 用户输入 6 位校验码后开始后台抓取 20 分钟日志并上传。</p>
 */
public class LocalFetchSecretActivity extends Activity {

    private static final String TAG = "Log_LocalFetchSecretActivity";

    private EditText mSecretCodeEdit;
    private Button mCancelBtn;
    private Button mConfirmUploadBtn;

    private final View.OnClickListener myUploadListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            judgeSecretCode();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_fetch_sectet);
        this.mSecretCodeEdit = (EditText) findViewById(R.id.edit_secret_code);
        this.mCancelBtn = (Button) findViewById(R.id.btn_cancel);
        this.mConfirmUploadBtn = (Button) findViewById(R.id.btn_confirm_upload);
        this.mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LogUtil.d(TAG, "本地触发日志，选择了取消不上传.");
                finish();
            }
        });
        this.mConfirmUploadBtn.setOnClickListener(this.myUploadListener);
    }

    /**
     * 校验用户输入的暗码。
     *
     * <p>校验码 = "绑定号 + 当天日期(yyyyMMdd)" 取摘要后，过滤掉非数字字符，
     * 取前 6 位。这样每天都会变，且不需要联网。</p>
     */
    private void judgeSecretCode() {
        String input = this.mSecretCodeEdit.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, getString(R.string.tip_input_secret_code), 1).show();
            return;
        }
        String today = DateUtils.format("yyyyMMdd",
                Calendar.getInstance(TimeZone.getTimeZone("GMT+8"), Locale.CHINESE).getTime());
        String bindNumber = SystemPropertiesUtils.get(SystemConstants.PROP_BIND_NUMBER, "");
        Matcher matcher = Pattern.compile("[^0-9]").matcher(Md5Utils.md5(bindNumber + today));
        String expected = null;
        if (matcher.find()) {
            try {
                expected = matcher.replaceAll("").trim().substring(0, 6);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LogUtil.d(TAG, "judge bindNumber:" + bindNumber + ", date:" + today + ", code:" + expected);
        if (!input.equals(expected)) {
            Toast.makeText(this, getString(R.string.tip_input_secret_code), 1).show();
            return;
        }
        LogUtil.d(TAG, "本地触发日志，开始后台抓取20min...");
        startFetchModemLog();
        Toast.makeText(this, getString(R.string.tip_upload_background), 1).show();
        finish();
    }

    /** 立刻触发一次 modem 日志抓取。 */
    private void startFetchModemLog() {
        ExtraMes extraMes = new ExtraMes();
        extraMes.setIsImmediately(true);
        ModemLogServeImpl.getInstance(getApplicationContext()).fetchingModemLog(extraMes);
    }
}
