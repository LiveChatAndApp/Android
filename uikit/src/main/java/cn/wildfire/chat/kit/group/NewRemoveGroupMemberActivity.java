/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class NewRemoveGroupMemberActivity extends WfcBaseActivity {
    private MenuItem menuItem;
    private TextView confirmTv;

    private GroupInfo groupInfo;

    public static final int RESULT_ADD_SUCCESS = 2;
    public static final int RESULT_ADD_FAIL = 3;

    private PickUserViewModel pickUserViewModel;
    private GroupViewModel groupViewModel;
    private Observer<UIUserInfo> contactCheckStatusUpdateLiveDataObserver = new Observer<UIUserInfo>() {
        @Override
        public void onChanged(@Nullable UIUserInfo userInfo) {
            List<UIUserInfo> list = pickUserViewModel.getCheckedUsers();
            if (list == null || list.isEmpty()) {
                menuItem.setTitle("删除");
                menuItem.setEnabled(false);
            } else {
                menuItem.setTitle("删除(" + list.size() + ")");
                menuItem.setEnabled(true);
            }
        }
    };

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        ArrayList<UserInfo> array = getIntent().getParcelableArrayListExtra("userInfos");
        if (groupInfo == null && array == null) {
            finish();
            return;
        }
        Fragment fragment = NewRemoveGroupMemberFragment.newInstance(array);
        pickUserViewModel = new ViewModelProvider(this).get(PickUserViewModel.class);
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(contactCheckStatusUpdateLiveDataObserver);
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected int menu() {
        return R.menu.group_remove_member;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.remove);
        super.afterMenus(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.remove) {
            removeMember();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(contactCheckStatusUpdateLiveDataObserver);
    }


    void removeMember() {
        if (groupInfo == null) {
            List<UIUserInfo> checkedUsers = pickUserViewModel.getCheckedUsers();
            ArrayList<String> userInfos = new ArrayList<>();
            for (UIUserInfo info : checkedUsers) {
                userInfos.add(info.getUserInfo().uid);
            }
            Intent intent = new Intent();
            intent.putStringArrayListExtra("userInfos", userInfos);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

}
