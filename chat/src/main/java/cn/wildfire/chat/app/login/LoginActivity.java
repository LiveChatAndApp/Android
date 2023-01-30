/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityOptionsCompat;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.login.model.LoginResult;
import cn.wildfire.chat.app.main.MainActivity;
import cn.wildfire.chat.app.setting.ForgetPasswordActivity;
import cn.wildfire.chat.app.setting.ResetPasswordActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.chat.R;

public class LoginActivity extends WfcBaseNoToolbarActivity {

    private final int PASSWORD_LOGIN = 1;
    private final int SMS_LOGIN = 2;
    private int mLoginType = PASSWORD_LOGIN;

    @BindView(R.id.passwordLoginGroup)
    Group passwordLoginGroup; // 密码login group
    @BindView(R.id.smsLoginGroup)
    Group smsLoginGroup; // 验证码login group

    @BindView(R.id.passwordLoginTextView)
    TextView passwordLoginTextView; // 密码登录
    @BindView(R.id.authCodeLoginTextView)
    TextView authCodeLoginTextView; // 验证码登录/注册

    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.phoneNumberEditText)
    EditText accountEditText; // 请输入手机号(仅支持中国大陆手机号)
    @BindView(R.id.passwordEditText)
    EditText passwordEditText; // 请输入密码
    @BindView(R.id.forgotPasswordTextView)
    TextView forgotPasswordTextView; // 忘记密码

    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText; // 请输入验证码
    @BindView(R.id.inviteCodeEditText)
    EditText inviteCodeEditText; // 请输入邀请码
    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton; // 获取验证码 button

    private Handler handler = new Handler();

    @Override
    protected int contentLayout() {
        return R.layout.login_activity_password;
    }

    @Override
    protected void afterViews() {
        setStatusBarTheme(this, false);
        setStatusBarColor(R.color.gray14);
        initUnderLine();
        if (getIntent().getBooleanExtra("isKickedOff", false)) {
            new MaterialDialog.Builder(this)
                    .content("你的账号已在其他手机登录")
                    .negativeText("知道了")
                    .build()
                    .show();
        }
        // 先暫時取消
//        SharedPreferences sp = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
//        boolean hasRegister = sp.getBoolean("hasRegister", false);
//        changeLoginLayout(hasRegister ? PASSWORD_LOGIN : SMS_LOGIN);
        changeLoginLayout(PASSWORD_LOGIN);
    }

    private void initUnderLine() {
        forgotPasswordTextView.setText(setUnderLine("忘记密码"));
    }

    private void changeLoginLayout(int loginType) {
        mLoginType = loginType;
        if (loginType == PASSWORD_LOGIN) { // 切换密码登录 layout
            passwordLoginGroup.setVisibility(View.VISIBLE);
            smsLoginGroup.setVisibility(View.GONE);
            authCodeLoginTextView.setTextColor(getResources().getColor(R.color.gray26));
            passwordLoginTextView.setTextColor(getResources().getColor(R.color.blueBtn));
            authCodeLoginTextView.setText("注册");
            passwordLoginTextView.setText(setUnderLine("登录"));
            loginButton.setText("登录");
        } else { // 切换验证码登录/注册 layout
            passwordLoginGroup.setVisibility(View.GONE);
            smsLoginGroup.setVisibility(View.VISIBLE);
            authCodeLoginTextView.setTextColor(getResources().getColor(R.color.blueBtn));
            passwordLoginTextView.setTextColor(getResources().getColor(R.color.gray26));
            authCodeLoginTextView.setText(setUnderLine("注册"));
            passwordLoginTextView.setText("登录");
            loginButton.setText("注册");
        }
        inputAccount(accountEditText.getText()); // 重置按钮状态
    }

    private SpannableString setUnderLine(String mString) {
        SpannableString mSpannableString = new SpannableString(mString);
        mSpannableString.setSpan(new UnderlineSpan(), 0, mSpannableString.length(), 0);
        return mSpannableString;
    }

    // 输入电话号码
    @OnTextChanged(value = R.id.phoneNumberEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputAccount(Editable editable) {
        String phone = editable.toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 判断是哪一种登录
        if (mLoginType == PASSWORD_LOGIN) {
            if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(password)) {
                loginButton.setEnabled(true);
            } else {
                loginButton.setEnabled(false);
            }
        } else {
            if (phone.length() == 11) {
                requestAuthCodeButton.setEnabled(true);
                if (authCodeEditText.getText().toString().trim().length() > 2) {
                    loginButton.setEnabled(true);
                } else {
                    loginButton.setEnabled(false);
                }
            } else {
                requestAuthCodeButton.setEnabled(false);
                loginButton.setEnabled(false);
            }
        }
    }

    // 输入密码
    @OnTextChanged(value = R.id.passwordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputPassword(Editable editable) {
        String phone = accountEditText.toString().trim();
        String password = editable.toString().trim();

        if (!TextUtils.isEmpty(phone) && !TextUtils.isEmpty(password)) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    // 切换验证码登录/注册 layout
    @OnClick(R.id.authCodeLoginTextView)
    void authCodeLogin() {
        changeLoginLayout(SMS_LOGIN);
    }

    // 获取 sms验证码
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
        String phoneNumber = accountEditText.getText().toString().trim();

        AppService.Instance().requestAuthCode(phoneNumber, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                Toast.makeText(LoginActivity.this, "发送验证码成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(LoginActivity.this, "发送验证码失败: " + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 输入 sms验证码
    @OnTextChanged(value = R.id.authCodeEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputAuthCode(Editable editable) {
        if (editable.toString().length() > 2) {
            loginButton.setEnabled(true);
        }
    }

    // 切换密码登录 layout
    @OnClick(R.id.passwordLoginTextView)
    void passwordLogin() {
        changeLoginLayout(PASSWORD_LOGIN);
    }

    // 登录click
    @OnClick(R.id.loginButton)
    void login() {
        if (mLoginType == PASSWORD_LOGIN) {
            // 密码登录
            passwordLoginClick();
        } else {
            // 注册
            smsLoginClick();
        }
    }

    // 忘记密码click
    @OnClick(R.id.forgotPasswordTextView)
    void forgotPasswordClick() {
        Intent resetPasswordIntent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(resetPasswordIntent, bundle);
        finish();
    }

    /**
     * 密码登录
     */
    private void passwordLoginClick() {
        String account = accountEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("登录中...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();

        AppService.Instance().passwordLogin(account, password, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }

                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                boolean connect = ChatManagerHolder.gChatManager.connect(loginResult.getUserId(), loginResult.getToken());
                LogHelper.e("login", "ChatManager.connect = " + connect);
                SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
                sp.edit()
                        .putString("id", loginResult.getUserId())
                        .putString("token", loginResult.getToken())
                        .apply();
                SharedPreferences sp2 = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                sp2.edit()
                        .putString("phone", account)
                        .putBoolean("createGroupEnable", loginResult.isCreateGroupEnable())
                        .apply();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(LoginActivity.this, "网络出问题了:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 验证码登录/注册
     */
    private void smsLoginClick() {
        String phoneNumber = accountEditText.getText().toString().trim();
        String authCode = authCodeEditText.getText().toString().trim();
        String inviteCode = inviteCodeEditText.getText().toString().trim();

        loginButton.setEnabled(false);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("注册登录中...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        AppService.Instance().smsLogin(inviteCode, phoneNumber, authCode, new AppService.LoginCallback() {
            @Override
            public void onUiSuccess(LoginResult loginResult) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                ChatManagerHolder.gChatManager.connect(loginResult.getUserId(), loginResult.getToken());
                SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
                sp.edit()
                        .putString("id", loginResult.getUserId())
                        .putString("token", loginResult.getToken())
                        .apply();
                SharedPreferences sp2 = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                sp2.edit()
                        .putString("phone", phoneNumber)
                        .putBoolean("createGroupEnable", loginResult.isCreateGroupEnable())
                        .putBoolean("isFirstRegisterLogin", true)
                        .apply();

                Intent resetPasswordIntent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                resetPasswordIntent.putExtra("resetCode", loginResult.getResetCode());
                Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                        android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                startActivity(resetPasswordIntent, bundle);

                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(LoginActivity.this, "登录失败：" + code + " " + msg, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loginButton.setEnabled(true);
            }
        });
    }
}
