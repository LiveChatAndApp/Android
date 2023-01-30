/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

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
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.ModifyGroupInfoType;

public class SetGroupNameActivity extends WfcBaseActivity {
    @BindView(R2.id.nameEditText)
    EditText nameEditText;
    @BindView(R2.id.clearTextImageView)
    ImageView clearTextImageView;

    private MenuItem confirmMenuItem;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    // 创建群组新流程，可以先设定名称
    private String groupName;

    public static final int RESULT_SET_GROUP_NAME_SUCCESS = 100;

    @Override
    protected int contentLayout() {
        return R.layout.group_set_name_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        groupName = getIntent().getStringExtra("name");
        if (groupInfo == null && TextUtils.isEmpty(groupName)) {
            finish();
            return;
        }
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        if (groupInfo != null) {
            nameEditText.setText(groupInfo.name);
            nameEditText.setSelection(groupInfo.name.length());
        } else {
            nameEditText.setText(groupName);
        }
        nameEditText.setFilters(new InputFilter[]{new EmojiDisableFilter(this)});
    }

    @Override
    protected int menu() {
        return R.menu.group_set_group_name;
    }

    @Override
    protected void afterMenus(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.confirm);
        if (nameEditText.getText().toString().trim().length() > 0) {
            confirmMenuItem.setEnabled(true);
        } else {
            confirmMenuItem.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            if (nameEditText.getText().toString().length() > 20) {
                Toast.makeText(this,R.string.group_name_max_length,Toast.LENGTH_SHORT).show();
                return true;
            }
            setGroupName();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnTextChanged(R2.id.nameEditText)
    void onTextChanged() {
        if (confirmMenuItem != null) {
            confirmMenuItem.setEnabled(nameEditText.getText().toString().trim().length() > 0);
        }
    }

    @OnClick(R2.id.clearTextImageView)
    void clearText() {
        nameEditText.setText("");
    }

    private void setGroupName() {
        if (groupInfo == null) {
            // 创建聊天室 进来修改名称
            Intent intent = new Intent();
            intent.putExtra("name", nameEditText.getText().toString());
            setResult(RESULT_OK, intent);
            finish();
            return;
        }
        // 从群组聊天室 进来更改名称
        groupInfo.name = nameEditText.getText().toString().trim();
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("请稍后...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        groupViewModel.modifyGroupInfo(groupInfo.target, ModifyGroupInfoType.Modify_Group_Name, groupInfo.name, null, Collections.singletonList(0)).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult operateResult) {
                dialog.dismiss();
                if (operateResult.isSuccess()) {
                    Toast.makeText(SetGroupNameActivity.this, "修改群名称成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("groupName", groupInfo.name);
                    setResult(RESULT_SET_GROUP_NAME_SUCCESS, intent);
                    finish();
                } else {
                    Toast.makeText(SetGroupNameActivity.this, "修改群名称失败: " + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
