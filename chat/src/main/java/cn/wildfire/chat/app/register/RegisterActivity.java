package cn.wildfire.chat.app.register;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.imageview.ShapeableImageView;

import butterknife.BindView;
import cn.wildfire.chat.kit.WfcBaseNoToolbarActivity;
import cn.wildfirechat.chat.R;

public class RegisterActivity extends WfcBaseNoToolbarActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.requestAuthCodeButton)
    TextView requestAuthCodeButton; // 获取验证码

    @BindView(R.id.inviteCodeEditText)
    EditText inviteCodeEditText; // 邀请码
    @BindView(R.id.iconImageView)
    ShapeableImageView iconImageView; // 头像
    @BindView(R.id.nickNameEditText)
    EditText nickNameEditText; // 昵称
    @BindView(R.id.phoneNumberEditText)
    EditText phoneNumberEditText; // phone number
    @BindView(R.id.authCodeEditText)
    EditText authCodeEditText; // 验证码
    @BindView(R.id.accountEditText)
    EditText accountEditText; // 账号
    @BindView(R.id.passwordEditText)
    EditText passwordEditText; // 密码
    @BindView(R.id.passwordConfirmEditText)
    EditText passwordConfirmEditText; // 密码确认

    @Override
    protected int contentLayout() {
        return R.layout.register_activity;
    }

    @Override
    protected void afterViews() {
        toolbar.setTitle(getString(R.string.register_title));
    }
}
