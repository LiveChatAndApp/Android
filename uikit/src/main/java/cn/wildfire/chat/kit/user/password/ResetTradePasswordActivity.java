/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user.password;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.model.UserInfo;

/**
 * 设置支付密码 (从未设置过支付密码)
 */
public class ResetTradePasswordActivity extends WfcBaseActivity {
    @BindView(R2.id.confirmButton)
    Button confirmButton;

    @BindView(R2.id.newPasswordEditText)
    EditText newPasswordEditText;
    @BindView(R2.id.confirmPasswordEditText)
    EditText confirmPasswordEditText;

    @BindView(R2.id.newPasswordTextView)
    TextView newPasswordTextView;
    @BindView(R2.id.confirmPasswordTextView)
    TextView confirmPasswordTextView;
    @BindView(R2.id.newShowImageView)
    ImageView newShowImageView;
    @BindView(R2.id.confirmShowImageView)
    ImageView confirmShowImageView;

    private int newPasswordImg = R.mipmap.ic_password_gone;
    private int confirmPasswordImg = R.mipmap.ic_password_gone;

    private String phone = null;
    private UserInfo userInfo;
    private TradePasswordViewModel viewModel;

    @Override
    protected int contentLayout() {
        return R.layout.reset_trade_password_activity;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void afterViews() {
        viewModel = new ViewModelProvider(this).get(TradePasswordViewModel.class);
        phone = getIntent().getStringExtra("phone");
        userInfo = getIntent().getParcelableExtra("userInfo");
        // 隐藏密码
        setPassHiddenStyle(newPasswordEditText);
        setPassHiddenStyle(confirmPasswordEditText);
    }

    @OnTextChanged(value = R2.id.newPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void newPassword(Editable editable) {
        if (!TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R2.id.confirmPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void confirmPassword(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnClick(R2.id.confirmButton)
    void resetPassword() {
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        // 字数须介于 4 至 20 字元）
        if (newPassword.length() < 4 || newPassword.length() > 20) {
            Toast.makeText(this, R.string.password_length_4_20_text, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.equals(newPassword, confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("正在设定支付密码...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();
        viewModel.resetTradePassword(newPassword, confirmPassword, "", new SimpleCallback<StatusResult>() {

            @Override
            public void onUiSuccess(StatusResult result) {
                if (isFinishing()) {
                    return;
                }
                // 如果是注册帐号进来初始化密码，则更改状态为 帐号以注册
                if (!TextUtils.isEmpty(getIntent().getStringExtra("resetCode"))) {
                    SharedPreferences sp = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                    sp.edit()
                            .putBoolean("hasRegister", true)
                            .apply();
                }
                String msg = "支付密码设置成功";

                Toast.makeText(ResetTradePasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(ResetTradePasswordActivity.this, "设置支付密码失败:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick({R2.id.newShowImageView, R2.id.confirmShowImageView})
    void showPassword(View view) {
        EditText enterView = null;
        ImageView showImgView = null;
        int img = 0;
        int id = view.getId();
        if (id == R.id.newShowImageView) {
            img = newPasswordImg;
            showImgView = newShowImageView;
            enterView = newPasswordEditText;
            newPasswordImg = img == R.mipmap.ic_password_show ? R.mipmap.ic_password_gone : R.mipmap.ic_password_show;
        } else if (id == R.id.confirmShowImageView) {
            img = confirmPasswordImg;
            showImgView = confirmShowImageView;
            enterView = confirmPasswordEditText;
            confirmPasswordImg = img == R.mipmap.ic_password_show ? R.mipmap.ic_password_gone : R.mipmap.ic_password_show;
        }
        if (img == R.mipmap.ic_password_show) {
            // 隐藏密码
            showImgView.setImageResource(R.mipmap.ic_password_gone);
            setPassHiddenStyle(enterView);
        } else {
            // 显示密码
            showImgView.setImageResource(R.mipmap.ic_password_show);
            enterView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }
    }
}
