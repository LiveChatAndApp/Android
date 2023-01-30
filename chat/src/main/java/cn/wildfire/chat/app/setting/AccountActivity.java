/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.password.ChangeTradePasswordActivity;
import cn.wildfire.chat.kit.user.password.ResetTradePasswordActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public class AccountActivity extends WfcBaseActivity {

    @BindView(R.id.changeTradePasswordOptionItemView)
    OptionItemView changeTradePasswordOptionItemView;

    private int REQUEST_TRADE = 100;
    private UserInfo userInfo;

    @Override
    protected int contentLayout() {
        return R.layout.account_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        userInfo = getIntent().getParcelableExtra("userInfo");
        changeTradeButtonName();
    }

    private void changeTradeButtonName() {
        if (userInfo != null && userInfo.hasTradePwd == 1) {
            changeTradePasswordOptionItemView.setTitle("修改支付密码");
        } else {
            changeTradePasswordOptionItemView.setTitle("设置支付密码");
        }
    }

    // 修改密码
    @OnClick(R.id.changePasswordOptionItemView)
    void changePassword() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .customView(R.layout.item_password_dialog, false)
                .build();
        TextView text = dialog.getCustomView().findViewById(cn.wildfire.chat.kit.R.id.text);
        TextView text2 = dialog.getCustomView().findViewById(cn.wildfire.chat.kit.R.id.text2);

        text.setOnClickListener((view) -> {
            dialog.dismiss();

            String phone = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE).getString("phone", "");
            Intent intent = new Intent(AccountActivity.this, ResetPasswordActivity.class);
            intent.putExtra("phone", phone);
            intent.putExtra("onlyClose", true);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
        });
        text2.setOnClickListener((view) -> {
            dialog.dismiss();
            Intent intent = new Intent(AccountActivity.this, ChangePasswordActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(intent, bundle);
        });
        dialog.getWindow().getDecorView().setBackground(null);
        dialog.show();
    }

    // 设置支付密码
    @OnClick(R.id.changeTradePasswordOptionItemView)
    void changeTradePassword() {
        if (userInfo == null) {
            Toast.makeText(getContext(), R.string.no_user_info, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 有区分 已设置或 未设置 , 待开发
        String phone = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE).getString("phone", "");

        // ResetTradePasswordActivity
        // ChangeTradePasswordActivity
        Intent intent;
        if (userInfo.hasTradePwd == 1) {
            intent = new Intent(AccountActivity.this, ChangeTradePasswordActivity.class);
        } else {
            intent = new Intent(AccountActivity.this, ResetTradePasswordActivity.class);
        }
        intent.putExtra("phone", phone);
        intent.putExtra("userInfo", userInfo);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivityForResult(intent, REQUEST_TRADE, bundle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TRADE && resultCode == RESULT_OK) {
            userInfo.hasTradePwd = 1;
            changeTradeButtonName();
        }
    }

    private void getRestCode() {
        String phone = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE).getString("phone", "");

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("准备修改密码...")
                .progress(true, 10)
                .cancelable(false)
                .build();
        dialog.show();

        AppService.Instance().requestResetAuthCode(phone, new AppService.SendCodeCallback() {
            @Override
            public void onUiSuccess() {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                Intent resetPasswordIntent = new Intent(AccountActivity.this, ResetPasswordActivity.class);
                resetPasswordIntent.putExtra("phone", phone);
                resetPasswordIntent.putExtra("onlyClose", true);
                Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                        android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
                startActivity(resetPasswordIntent, bundle);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(AccountActivity.this, "网络出问题了:" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
