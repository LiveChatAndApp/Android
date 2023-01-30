/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.king.zxing.Intents;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UserInfo;

/**
 * 聊天室详情
 */
public class ConversationInfoActivity extends WfcBaseActivity {
    public static final int REQUEST_CREATE_GROUP = 100;
    private ConversationInfo conversationInfo;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        conversationInfo = getIntent().getParcelableExtra("conversationInfo");
        Fragment fragment = null;
        switch (conversationInfo.conversation.type) {
            case Single:
                fragment = SingleConversationInfoFragment.newInstance(conversationInfo);
                break;
            case Group:
                setTitle(R.string.conversation_group_info_title);
                fragment = GroupConversationInfoFragment.newInstance(conversationInfo);
                break;
            case ChatRoom:
                // TODO
                break;
            case Channel:
                fragment = ChannelConversationInfoFragment.newInstance(conversationInfo);
                break;
            case SecretChat:
                fragment = SecretConversationInfoFragment.newInstance(conversationInfo);
                break;
            default:
                break;
        }
        if (fragment == null) {
            // 還沒做
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        switch (requestCode) {
            case REQUEST_CREATE_GROUP:
                setResult(RESULT_OK);
                finish();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
