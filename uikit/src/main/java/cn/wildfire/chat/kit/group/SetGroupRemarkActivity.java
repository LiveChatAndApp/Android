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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.utils.EmojiDisableFilter;
import cn.wildfirechat.model.GroupInfo;

public class SetGroupRemarkActivity extends WfcBaseActivity {
    @BindView(R2.id.remarkEditText)
    EditText remarkEditText;

    private MenuItem confirmMenuItem;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    @Override
    protected int contentLayout() {
        return R.layout.group_set_remark_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);

        if (!TextUtils.isEmpty(groupInfo.remark)) {
            remarkEditText.setText(groupInfo.remark);
            remarkEditText.setSelection(groupInfo.remark.length());
        }
        remarkEditText.setFilters(new InputFilter[]{new EmojiDisableFilter(this)});
    }

    @Override
    protected int menu() {
        return R.menu.group_set_group_remark;
    }

    @Override
    protected void afterMenus(Menu menu) {
//        confirmMenuItem = menu.findItem(R.id.confirm);
//        if (remarkEditText.getText().toString().trim().length() > 0) {
//            confirmMenuItem.setEnabled(true);
//        } else {
//            confirmMenuItem.setEnabled(false);
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            if (remarkEditText.getText().toString().length() > 20) {
                Toast.makeText(this,R.string.group_name_max_length,Toast.LENGTH_SHORT).show();
                return true;
            }
            setGroupRemark();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnTextChanged(R2.id.remarkEditText)
    void onTextChanged() {
//        if (confirmMenuItem != null) {
//            confirmMenuItem.setEnabled(remarkEditText.getText().toString().trim().length() > 0);
//        }
    }

    private void setGroupRemark() {
        groupInfo.remark = remarkEditText.getText().toString().trim();
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("?????????...")
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();

        groupViewModel.setGroupRemark(groupInfo.target, groupInfo.remark).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult operateResult) {
                dialog.dismiss();
                if (operateResult.isSuccess()) {
                    Toast.makeText(SetGroupRemarkActivity.this, "?????????????????????", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(SetGroupRemarkActivity.this, "?????????????????????: " + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
