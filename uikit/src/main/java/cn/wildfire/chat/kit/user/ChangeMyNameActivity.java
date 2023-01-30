/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.utils.EmojiDisableFilter;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_DisplayName;

/**
 * 設定個人姓名
 */
public class ChangeMyNameActivity extends WfcBaseActivity {

    @BindView(R2.id.confirmButton)
    Button confirmButton;
    @BindView(R2.id.nameEditText)
    EditText nameEditText;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    @Override
    protected void afterViews() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if (userInfo == null) {
            Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
        initView();
    }

    private void initView() {
        if (userInfo != null) {
            nameEditText.setText(userInfo.nickName);
        }
        nameEditText.setSelection(nameEditText.getText().toString().trim().length());
        nameEditText.setFilters(new InputFilter[]{new EmojiDisableFilter(this)});
    }

    @Override
    protected int contentLayout() {
        return R.layout.user_change_my_name_activity;
    }

    @Override
    public void onBackPressed() {
        try {
            String name = nameEditText.getText().toString();
            if (name.equals(userInfo.nickName)) {
                super.onBackPressed();
            } else {
                showCheckDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCheckDialog() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.check_save_title_dialog)
                .content(R.string.check_save_content_dialog)
                .positiveText(R.string.close_screen)
                .negativeText(R.string.save)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        changeMyName();
                    }
                })
                .build();
        dialog.show();
    }

    //    @Override
//    protected int menu() {
//        return R.menu.user_change_my_name;
//    }
//
//    @Override
//    protected void afterMenus(Menu menu) {
//        confirmMenuItem = menu.findItem(R.id.save);
//        confirmMenuItem.setEnabled(false);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.save) {
//            changeMyName();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @OnTextChanged(value = R2.id.nameEditText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void nameChange(CharSequence text, int start, int before, int count) {
        if (count == 0) {
            return;
        }
        char[] chars = text.toString().toCharArray();
        char newChar = chars[start];

        // 連續兩個空白
        if (newChar == 32 && start != 0 && chars[start - 1] == 32) {
            StringBuilder sb = new StringBuilder(text);
            nameEditText.setText(sb.deleteCharAt(start));
        } else if (newChar == 32 && start != text.length() - 1 && chars[start + 1] == 32) {
            StringBuilder sb = new StringBuilder(text);
            nameEditText.setText(sb.deleteCharAt(start));
        }
    }

    @OnClick(R2.id.confirmButton)
    void confirmButtonClick() {
        changeMyName();
    }

    // 储存
    private void changeMyName() {
        if (nameEditText.getText().toString().length() > 20) {
            Toast.makeText(this, R.string.group_name_max_length, Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("修改中...")
                .progress(true, 100)
                .build();
        dialog.show();

        String nickName = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(nickName)) {
            Toast.makeText(this, "昵称栏位不能为空白。", Toast.LENGTH_SHORT).show();
            return;
        }

        ModifyMyInfoEntry entry = new ModifyMyInfoEntry(Modify_DisplayName, nickName);
        userViewModel.modifyMyInfo(Collections.singletonList(entry)).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(ChangeMyNameActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChangeMyNameActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
                finish();
            }
        });
    }
}
