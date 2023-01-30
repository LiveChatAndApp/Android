/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_Gender;

import android.widget.Button;
import android.widget.Spinner;
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
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

public class ChangeGenderActivity extends WfcBaseActivity {

    @BindView(R2.id.confirmButton)
    Button confirmButton;
    @BindView(R2.id.genderSpinner)
    Spinner genderSpinner;

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
        int index = userInfo.gender - 1;
        if (index < 0) {
            index = 0;
        }
        genderSpinner.setSelection(index);
    }

    @Override
    protected int contentLayout() {
        return R.layout.change_gender_activity;
    }

    @Override
    public void onBackPressed() {
        try {
            int gender = userInfo.gender;
            int spinnerValue = genderSpinner.getSelectedItemPosition()+1;
            if (spinnerValue == gender) {
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
                        saveData();
                    }
                })
                .build();
        dialog.show();
    }

    private void saveData() {
        int spinnerValue = genderSpinner.getSelectedItemPosition() + 1;
        changeMyGender("" + spinnerValue);
    }

    @OnClick(R2.id.confirmButton)
    void confirmButtonClick() {
        saveData();
    }

    // 储存 gender
    private void changeMyGender(String gender) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("修改中...")
                .progress(true, 100)
                .build();
        dialog.show();

        ModifyMyInfoEntry entry = new ModifyMyInfoEntry(Modify_Gender, gender);
        userViewModel.modifyMyInfo(Collections.singletonList(entry)).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(ChangeGenderActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChangeGenderActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
                finish();
            }
        });
    }
}
