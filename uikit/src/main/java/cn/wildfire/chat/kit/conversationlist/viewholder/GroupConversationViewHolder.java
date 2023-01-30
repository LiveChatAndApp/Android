/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@ConversationInfoType(type = Conversation.ConversationType.Group, line = 0)
@EnableContextMenu
public class GroupConversationViewHolder extends ConversationViewHolder {

    public GroupConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        GroupInfo groupInfo = ChatManagerHolder.gChatManager.getGroupInfo(conversationInfo.conversation.target, false);
        int index = 0;
        while ((groupInfo == null || groupInfo instanceof NullGroupInfo) && index <= 3) {
            index++;
            groupInfo = ChatManagerHolder.gChatManager.getGroupInfo(conversationInfo.conversation.target, true);
        }
        String name;
        String portrait;
        String count = "";

        if (groupInfo != null) {
            name = cutGroupTitle(groupInfo);
            // 如果是广播群，则不使用 合成头像
            if (groupInfo.isBroadCastGroup()) {
                // 不使用合成头像，使用预设头像
                portrait = TextUtils.isEmpty(groupInfo.portrait) ? "" : groupInfo.portrait;
            } else {
                // 一般群组要显示 人数
                List<GroupMember> list = ChatManagerHolder.gChatManager.getGroupMembers(groupInfo.target, false);
                if (list != null && list.size() > 0) {
                    count = "(" + list.size() + ")";
                }

                // 一般群组 如果没有头像，则使用合成头像
                if (TextUtils.isEmpty(groupInfo.portrait)) {
                    portrait = ImageUtils.getGroupGridPortrait(getFragment().getContext(), conversationInfo.conversation.target, 60);
                } else {
                    portrait = groupInfo.portrait;
                }

            }
        } else {
            name = "群聊";
            portrait = null;
        }

        if (TextUtils.isEmpty(portrait)) {
            ChatManager.Instance().getWorkHandler().post(() -> {
                List<GroupMember> list = ChatManagerHolder.gChatManager.getGroupMembers(conversationInfo.conversation.target, false);
                Bitmap bitmap = getBitmap(conversationInfo.conversation.target, list);
                ChatManagerHolder.gChatManager.getMainHandler().post(() -> {
                    GlideUtil.load(fragment, bitmap).
                            placeholder(R.mipmap.ic_group_default_portrait)
                            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
                            .into(portraitImageView);
                });
            });
        } else {
            GlideUtil.load(fragment, portrait).
                    placeholder(R.mipmap.ic_group_default_portrait)
                    .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
                    .into(portraitImageView);
        }

        nameTextView.setText(name);
        memberCountTextView.setText(count);
    }

    // 如果群组名称太长则截断，另补 群组人数
    private String cutGroupTitle(GroupInfo groupInfo) {
        String title = (!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
        String text = "";
        String conversationTitle = "";
        try {
            float length = 0;
            int size = Math.min(title.length(), 15) + 1;
            do {
                size--;
                text = cutText(title, size);
                Paint paint = new Paint();
                paint.setTextSize(15);
                length = paint.measureText(text);
            } while (length > 150);
            conversationTitle = text;
        } catch (Exception e) {
            if (title.length() > 7) {
                title = title.substring(0, 6);
                title += "...";
            }
            conversationTitle = title;
        }
        return conversationTitle;
    }

    private String cutText(String text, int size) {
        String title = "";
        if (text.length() > size) {
            title = text.substring(0, size - 1);
            title += "...";
        } else {
            title = text;
        }
        return title;
    }

    private Bitmap getBitmap(String groupId, List<GroupMember> members) {
        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }
        List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
        return ImageUtils.generateNewGroupPortrait(getFragment().getContext(), userInfos, 60);
    }
}
