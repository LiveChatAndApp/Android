/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityOptionsCompat;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class ForgetPasswordActivity extends WfcBaseActivity {
    @BindView(R.id.confirmButton)
    Button confirmButton;
    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton;

    @BindView(R.id.phoneNumberEditText)
    EditText phoneNumberEditText;
    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;

    private Handler handler = new Handler();

    private boolean hasSendAuth = false;// 发送验证码

    @Override
    protected int contentLayout() {
        return R.layout.forget_password_activity;
    }

    @Override
    public void onBackPressed() {
        Intent intent;
        intent = new Intent(this, LoginActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(intent, bundle);
        finish();
    }

    @Override
    protected void afterViews() {
        String phone = getIntent().getStringExtra("phone");
        if (!TextUtils.isEmpty(phone)) {
            phoneNumberEditText.setText(phone);
        }
    }

    @OnTextChanged(value = R.id.phoneNumberEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void phoneNumberEdit(Editable editable) {
        if (!TextUtils.isEmpty(editable)) {
            requestAuthCodeButton.setEnabled(true);
        } else {
            requestAuthCodeButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void authCode(Editable editable) {
        if (hasSendAuth & !TextUtils.isEmpty(phoneNumberEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnClick(R.id.requestAuthCodeButton)
    void requestAuthCode() {
        hasSendAuth = true;
        String phone = phoneNumberEditText.getText().toString().trim();
        requestAuthCodeButton.setEnabled(false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    requestAuthCodeButton.setEnabled(true);
                }
            }
        }, 60 * 1000);

        Toast.makeText(this, "请求验证码...", Toast.LENGTH_SHORT).show();

        AppService.Instance().requestResetAuthCode(phone, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                phoneNumberEditText.setEnabled(false);
                Toast.makeText(ForgetPasswordActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(ForgetPasswordActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.confirmButton)
    void resetPassword() {
        String phone = phoneNumberEditText.getText().toString().trim();
        String resetCode = authCodeEditText.getText().toString().trim();

        Intent resetPasswordIntent = new Intent(ForgetPasswordActivity.this, ResetPasswordActivity.class);
        resetPasswordIntent.putExtra("phone", phone);
        resetPasswordIntent.putExtra("resetCode", resetCode);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(resetPasswordIntent, bundle);
        finish();
    }
}
