/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.CreateConversationActivity;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 群聊列表
 */
public class GroupListActivity extends WfcBaseActivity {
    private int REQUEST_CREATE_GROUP = 100;

    private boolean forResult;
    /**
     * intent里面置为{@code true}时，返回groupInfo，不直接打开群会话界面
     */
    public static final String INTENT_FOR_RESULT = "forResult";

    /**
     * for result时，单选，还是多选？
     */
    // TODO
    public static final String MODE_SINGLE = "single";
    public static final String MODE_MULTI = "multi";

    // TODO activity or fragment?
    public static Intent buildIntent(boolean pickForResult, boolean isMultiMode) {

        return null;
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected int menu() {
        return R.menu.group_list;
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        MenuItem chatItem = menu.findItem(R.id.chat);
        chatItem.setVisible(checkCreateGroupPermission());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.chat) {
            createConversation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkCreateGroupPermission() {
        SharedPreferences sp2 = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
        return sp2.getBoolean("createGroupEnable", true);
    }

    @Override
    protected void afterViews() {
        forResult = getIntent().getBooleanExtra(INTENT_FOR_RESULT, false);
        GroupListFragment fragment = new GroupListFragment();
        if (forResult) {
            fragment.setOnGroupItemClickListener(groupItem -> {
                Intent intent = new Intent();
                // TODO 多选
                GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(groupItem.gid, false);
                ArrayList<GroupInfo> groupInfos = new ArrayList<>();
                groupInfos.add(groupInfo);
                intent.putParcelableArrayListExtra("groupInfos", groupInfos);
                setResult(RESULT_OK, intent);
                finish();
            });
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    private void createConversation() {
        Intent intent = new Intent(this, CreateConversationActivity.class);
        startActivityForResult(intent, REQUEST_CREATE_GROUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_GROUP && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
