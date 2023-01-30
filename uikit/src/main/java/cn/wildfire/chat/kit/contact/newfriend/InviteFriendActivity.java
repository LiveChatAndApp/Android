/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 邀请成为朋友
 */
public class InviteFriendActivity extends WfcBaseActivity {
    @BindView(R2.id.introTextView)
    TextView introTextView;
    @BindView(R2.id.verifyRadioGroup)
    RadioGroup verifyRadioGroup;
    @BindView(R2.id.skipVerifyRadioButton)
    RadioButton skipVerifyRadioButton;
    @BindView(R2.id.shouldVerifyRadioButton)
    RadioButton shouldVerifyRadioButton;
    @BindView(R2.id.verifyEditText)
    TextView verifyEditText;
    @BindView(R2.id.verifyTipLabel)
    TextView verifyTipLabel;

    private UserInfo userInfo;
    private boolean hasVerify = false;

    @Override
    protected void afterViews() {
        super.afterViews();
        userInfo = getIntent().getParcelableExtra("userInfo");
        if (userInfo == null) {
            finish();
        }
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        UserInfo me = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        introTextView.setText("我是 " + ChatManager.Instance().getUserDisplayName(me));
    }

    @Override
    protected int contentLayout() {
        return R.layout.contact_invite_activity;
    }

    @SuppressLint("InvalidR2Usage")
    @OnCheckedChanged({R2.id.skipVerifyRadioButton, R2.id.shouldVerifyRadioButton})
    void checkChange(CompoundButton button, boolean checked) {
        if (checked) {
            int id = button.getId();
            if (id == R.id.skipVerifyRadioButton) {
                hasVerify = false;
                verifyEditText.setEnabled(false);
            } else if (id == R.id.shouldVerifyRadioButton) {
                hasVerify = true;
                verifyEditText.setEnabled(true);
            }
            verifyEditText.setVisibility(id == R.id.skipVerifyRadioButton? View.GONE:View.VISIBLE);
            verifyTipLabel.setVisibility(id == R.id.skipVerifyRadioButton? View.GONE:View.VISIBLE);
        }
    }

    @Override
    protected int menu() {
        return R.menu.contact_invite;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            invite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void invite() {
        String verifyText = verifyEditText.getText().toString();
        if (hasVerify && TextUtils.isEmpty(verifyText)) {
            Toast.makeText(this, getString(R.string.friend_verify_not_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        if(!hasVerify){
            verifyText = "";
        }
        ContactViewModel contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        contactViewModel.sendFriendRequest(userInfo.uid, hasVerify, verifyText, introTextView.getText().toString())
                .observe(this, result -> {
                    if (result.code == 0) {
                        Toast.makeText(InviteFriendActivity.this, R.string.send_friend_request, Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (result.code == 16) {
                        Toast.makeText(InviteFriendActivity.this, R.string.send_friend_request, Toast.LENGTH_SHORT).show();
                        finish();
                    }  else if (result.code == 1041) {
                        Toast.makeText(InviteFriendActivity.this, result.message, Toast.LENGTH_SHORT).show();
                        finish();
                    } else if (result.code != 0 && !TextUtils.isEmpty(result.message)) {
                        Toast.makeText(InviteFriendActivity.this, result.message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(InviteFriendActivity.this, R.string.send_friend_request_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
