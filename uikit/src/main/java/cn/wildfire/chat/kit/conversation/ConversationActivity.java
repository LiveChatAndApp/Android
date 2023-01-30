/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 聊天室
 */
public class ConversationActivity extends WfcBaseActivity {
    public static final int REQUEST_INFO = 100;
    private boolean isInitialized = false;
    private ConversationFragment conversationFragment;
    private Conversation conversation;
    private MenuItem conversationInfoItem;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    private void setConversationBackground() {
        // you can setup your conversation background here
//        getWindow().setBackgroundDrawableResource(R.mipmap.splash);
    }

    @Override
    protected void afterViews() {
        IMServiceStatusViewModel imServiceStatusViewModel = new ViewModelProvider(this).get(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, aBoolean -> {
            if (!isInitialized && aBoolean) {
                init();
                isInitialized = true;
            }
        });
        conversationFragment = new ConversationFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.containerFrameLayout, conversationFragment, "content")
                .commit();

        setConversationBackground();
    }

    @Override
    protected int menu() {
        return R.menu.conversation;
    }

    public ConversationFragment getConversationFragment() {
        return conversationFragment;
    }

    @Override
    protected void afterMenus(Menu menu) {
        super.afterMenus(menu);
        if (conversation != null && conversation.type == Conversation.ConversationType.ChatRoom) {
            conversationInfoItem = menu.findItem(R.id.menu_conversation_info);
            conversationInfoItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_conversation_info) {
            showConversationInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!conversationFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    private void showConversationInfo() {
        Intent intent = new Intent(this, ConversationInfoActivity.class);
        ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
        if (conversationInfo == null) {
            Toast.makeText(this, "获取会话信息失败", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra("conversationInfo", conversationInfo);
        startActivityForResult(intent,REQUEST_INFO);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        conversation = intent.getParcelableExtra("conversation");
        if (conversation == null) {
            finish();
            return;
        }
        long initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        String channelPrivateChatUser = intent.getStringExtra("channelPrivateChatUser");
        conversationFragment.setupConversation(conversation, null, initialFocusedMessageId, channelPrivateChatUser);
        if (conversation.type == Conversation.ConversationType.ChatRoom && conversationInfoItem != null) {
            conversationInfoItem.setVisible(false);
        }
    }


    private void init() {
        Intent intent = getIntent();
        conversation = intent.getParcelableExtra("conversation");
        String conversationTitle = intent.getStringExtra("conversationTitle");
        long initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        if (conversation == null) {
            finish();
            return;
        }
        conversationFragment.setupConversation(conversation, conversationTitle, initialFocusedMessageId, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        switch (requestCode) {
            case REQUEST_INFO:
                setResult(RESULT_OK);
                finish();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line) {
        return buildConversationIntent(context, type, target, line, -1);
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line, long toFocusMessageId) {
        Conversation conversation = new Conversation(type, target, line);
        return buildConversationIntent(context, conversation, null, toFocusMessageId);
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line, String channelPrivateChatUser) {
        Conversation conversation = new Conversation(type, target, line);
        return buildConversationIntent(context, conversation, null, -1);
    }

    public static Intent buildConversationIntent(Context context, Conversation conversation, String channelPrivateChatUser, long toFocusMessageId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("toFocusMessageId", toFocusMessageId);
        intent.putExtra("channelPrivateChatUser", channelPrivateChatUser);
        return intent;
    }
}
