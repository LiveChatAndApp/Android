/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

/**
 * 发起群聊 群聊选人
 */
public class CreateConversationActivity extends PickConversationTargetActivity {
    private int REQUEST_CREATE_GROUP = 100;
    private GroupViewModel groupViewModel;

    @Override
    protected void afterViews() {
        super.afterViews();
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean status = super.onPrepareOptionsMenu(menu);
        confirmTv.setText("下一步");
        return status;
    }

    protected void updatePickStatus(List<UIUserInfo> userInfos) {
        if (userInfos == null || userInfos.isEmpty()) {
            confirmTv.setText("下一步");
            menuItem.setEnabled(false);
        } else {
            confirmTv.setText("下一步(" + userInfos.size() + ")");
            menuItem.setEnabled(true);
        }
    }

    @Override
    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos) {
        List<String> initialCheckedIds = pickUserViewModel.getInitialCheckedIds();
        List<UserInfo> userInfos = null;
        if (initialCheckedIds != null && !initialCheckedIds.isEmpty()) {
            UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
            userInfos = userViewModel.getUserInfos(initialCheckedIds);
        }
        userInfos = userInfos == null ? new ArrayList<>() : userInfos;

        for (UIUserInfo uiUserinfo : newlyCheckedUserInfos) {
            userInfos.add(uiUserinfo.getUserInfo());
        }

        if (userInfos.size() == 1) {

            Intent intent = new Intent(this, ConversationActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfos.get(0).uid);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
            finish();
        } else {
            if (userInfos.size() == 0) {
                return;
            }
            ArrayList<UserInfo> arrayList = new ArrayList<>(userInfos);

            Intent intent = new Intent(this, CreateGroupActivity.class);
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(this,
                    android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            intent.putParcelableArrayListExtra("userInfos", arrayList);
            startActivityForResult(intent, REQUEST_CREATE_GROUP, bundle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CREATE_GROUP == requestCode && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
            return;
        }
    }

    private void addGroup2Fav(String groupID) {
        groupViewModel.setFavGroup(groupID, true).observe(this, result -> {
            Toast.makeText(this, getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CreateConversationActivity.this, ConversationActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupID, 0);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
        });
    }

    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent intent = new Intent(this, ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfos.get(0).target);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        finish();
    }
}
