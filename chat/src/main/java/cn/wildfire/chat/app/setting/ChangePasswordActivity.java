package cn.wildfire.chat.app.setting;

import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfirechat.chat.R;

public class ChangePasswordActivity extends WfcBaseActivity {
    @BindView(R.id.confirmButton)
    Button confirmButton;
    @BindView(R.id.oldPasswordEditText)
    EditText oldPasswordEditText;
    @BindView(R.id.newPasswordEditText)
    EditText newPasswordEditText;
    @BindView(R.id.confirmPasswordEditText)
    EditText confirmPasswordEditText;
    @BindView(R.id.oldShowImageView)
    ImageView oldShowImageView;
    @BindView(R.id.newShowImageView)
    ImageView newShowImageView;
    @BindView(R.id.confirmShowImageView)
    ImageView confirmShowImageView;

    private int oldPasswordImg = R.mipmap.ic_password_gone;
    private int newPasswordImg = R.mipmap.ic_password_gone;
    private int confirmPasswordImg = R.mipmap.ic_password_gone;

    @Override
    protected int contentLayout() {
        return R.layout.change_password_activity;
    }

    @Override
    protected void afterViews() {
//        setStatusBarTheme(this, false);
//        setStatusBarColor(R.color.gray14);

        // 隐藏密码
        setPassHiddenStyle(oldPasswordEditText);
        setPassHiddenStyle(newPasswordEditText);
        setPassHiddenStyle(confirmPasswordEditText);
    }

    @OnTextChanged(value = R.id.oldPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void oldPassword(Editable editable) {
        if (!TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.newPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void newPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(confirmPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.confirmPasswordEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void confirmPassword(Editable editable) {
        if (!TextUtils.isEmpty(oldPasswordEditText.getText()) && !TextUtils.isEmpty(newPasswordEditText.getText()) && !TextUtils.isEmpty(editable)) {
            confirmButton.setEnabled(true);
        } else {
            confirmButton.setEnabled(false);
        }
    }

    @OnClick(R.id.confirmButton)
    void resetPassword() {

        String oldPassword = oldPasswordEditText.getText().toString().trim();
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

        AppService.Instance().changePassword(oldPassword, newPassword, new SimpleCallback<StatusResult>() {

            @Override
            public void onUiSuccess(StatusResult result) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(ChangePasswordActivity.this, "修改密码成功", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();

                Toast.makeText(ChangePasswordActivity.this, "修改密码失败:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick({R.id.oldShowImageView, R.id.newShowImageView, R.id.confirmShowImageView})
    void showPassword(View view) {
        EditText enterView = null;
        ImageView showImgView = null;
        int img = 0;
        int id = view.getId();
        switch (id) {
            case R.id.oldShowImageView:
                img = oldPasswordImg;
                showImgView = oldShowImageView;
                enterView = oldPasswordEditText;
                oldPasswordImg = img == R.mipmap.ic_password_show ? R.mipmap.ic_password_gone : R.mipmap.ic_password_show;
                break;
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