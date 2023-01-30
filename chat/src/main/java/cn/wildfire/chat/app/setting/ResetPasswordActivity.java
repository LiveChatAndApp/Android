package cn.wildfire.chat.app.setting;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.chat.R;

public class ResetPasswordActivity extends WfcBaseActivity {
    @BindView(R.id.confirmButton)
    Button confirmButton;

    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText;
    @BindView(R.id.newPasswordEditText)
    EditText newPasswordEditText;
    @BindView(R.id.confirmPasswordEditText)
    EditText confirmPasswordEditText;

    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton;
    @BindView(R.id.newPasswordTextView)
    TextView newPasswordTextView;
    @BindView(R.id.confirmPasswordTextView)
    TextView confirmPasswordTextView;

    @BindView(R.id.authCodeFrameLayout)
    RelativeLayout authCodeFrameLayout;
    @BindView(R.id.contentLayout)
    LinearLayout contentLayout;
    @BindView(R.id.root)
    ConstraintLayout root;
    @BindView(R.id.newShowImageView)
    ImageView newShowImageView;
    @BindView(R.id.confirmShowImageView)
    ImageView confirmShowImageView;

    private String resetCode;
    private String phone = null;
    private boolean onlyClose = false;

    private int newPasswordImg = R.mipmap.ic_password_gone;
    private int confirmPasswordImg = R.mipmap.ic_password_gone;

    @Override
    protected int contentLayout() {
        return R.layout.reset_password_activity;
    }

    @Override
    public void onBackPressed() {
        if (onlyClose) {
            // personal account center, modifier password
            super.onBackPressed();
        } else if (TextUtils.isEmpty(phone)) {
            // sms register account, init password
            Intent intent;
            intent = new Intent(this, LoginActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
            finish();
        } else {
            // password login, forget password
            Intent intent;
            intent = new Intent(this, ForgetPasswordActivity.class);
            intent.putExtra("phone", phone);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
            finish();
        }
    }

    @Override
    protected void afterViews() {
        // register , forget password
        resetCode = getIntent().getStringExtra("resetCode");
        // forget password , account center to modifier password
        phone = getIntent().getStringExtra("phone");
        // account center to modifier password
        onlyClose = getIntent().getBooleanExtra("onlyClose", false);
        if (onlyClose) {
            // personal account center, modifier password
            authCodeFrameLayout.setVisibility(View.VISIBLE);
            requestAuthCodeButton.setVisibility(View.VISIBLE);
            setTitle("修改密码");
            newPasswordTextView.setText("设置新密码");
            newPasswordEditText.setHint("请输入新密码");
            confirmPasswordTextView.setText("确认密码");
            confirmPasswordEditText.setHint("请再次输入新密码");
            confirmButton.setText(R.string.save);
        }
        if (!onlyClose && TextUtils.isEmpty(phone)) {
            // register
            confirmButton.setText(R.string.complete);
            setTitle("设置密码");
        } else if (!onlyClose && !TextUtils.isEmpty(phone)) {
            // forget password
            setTitle("设置新密码");
            confirmButton.setText(R.string.save);
            newPasswordTextView.setText("设置新密码");
            newPasswordEditText.setHint("请输入新密码");
            confirmPasswordTextView.setText("确认新密码");
            confirmPasswordEditText.setHint("请再次输入新密码");
        }

        if (!TextUtils.isEmpty(resetCode)) {
            authCodeFrameLayout.setVisibility(View.GONE);
            root.setBackgroundColor(getResources().getColor(R.color.white));
        }

        // 隐藏密码
        setPassHiddenStyle(newPasswordEditText);
        setPassHiddenStyle(confirmPasswordEditText);
    }

    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void authCode(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.newPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void newPassword(Editable editable) {
        // account center to modify password
        if (onlyClose) {
            if (!TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
                requestAuthCodeButton.setEnabled(true);
            } else {
                requestAuthCodeButton.setEnabled(false);
            }
            return;
        }
        // register , forget password
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.confirmPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void confirmPassword(Editable editable) {
        // account center to modify password
        if (onlyClose) {
            if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
                requestAuthCodeButton.setEnabled(true);
            } else {
                requestAuthCodeButton.setEnabled(false);
            }
            return;
        }
        // register , forget password
        if ((!TextUtils.isEmpty(authCodeEditText.getText()) || !TextUtils.isEmpty(resetCode)) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    private Handler handler = new Handler();

    @OnClick(R.id.requestAuthCodeButton)
    void requestAuthCode() {
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
                Toast.makeText(ResetPasswordActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(ResetPasswordActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.confirmButton)
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
                .content("正在修改密码...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();

        String code = TextUtils.isEmpty(resetCode) ? authCodeEditText.getText().toString() : resetCode;

        AppService.Instance().resetPassword(phone, code, newPassword, new SimpleCallback<StatusResult>() {

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
                String msg;
                if (onlyClose) {
                    // personal account center, modifier password
                    msg = "重置密码成功";
                } else if (TextUtils.isEmpty(phone)) {
                    // sms register account, init password
                    msg = "密码设置成功";
                } else {
                    // password login, forget password
                    msg = "重置密码成功";
                }

                Toast.makeText(ResetPasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(ResetPasswordActivity.this, "重置密码失败:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick({R.id.newShowImageView, R.id.confirmShowImageView})
    void showPassword(View view) {
        EditText enterView = null;
        ImageView showImgView = null;
        int img = 0;
        int id = view.getId();
        switch (id) {
            case R.id.newShowImageView:
                img = newPasswordImg;
                showImgView = newShowImageView;
                enterView = newPasswordEditText;
                newPasswordImg = img == R.mipmap.ic_password_show ? R.mipmap.ic_password_gone : R.mipmap.ic_password_show;
                break;
            case R.id.confirmShowImageView:
                img = confirmPasswordImg;
                showImgView = confirmShowImageView;
                enterView = confirmPasswordEditText;
                confirmPasswordImg = img == R.mipmap.ic_password_show ? R.mipmap.ic_password_gone : R.mipmap.ic_password_show;
                break;
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