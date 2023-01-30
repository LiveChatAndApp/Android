/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.MESSAGE_SILENCE;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

/**
 * 发言太过频繁，禁止发言30秒
 */
@ContentTag(type = MESSAGE_SILENCE, flag = PersistFlag.Persist)
public class SilenceMessageContent extends NotificationMessageContent {
    public SilenceMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
//        显示屏蔽信息memo文字：
//        -自己看到的提示文字：您发送包含敏感词的内容
//        -别人看到的提示文字：<用户名称>发送包含敏感词的内容
        if (fromSelf) {
            return "发言太过频繁，禁止发言30秒。";
        } else {
            return "<" + ChatManager.Instance().getUserDisplayName(message.sender) + ">发言太过频繁，禁止发言30秒。";
        }
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    protected SilenceMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<SilenceMessageContent> CREATOR = new Creator<SilenceMessageContent>() {
        @Override
        public SilenceMessageContent createFromParcel(Parcel source) {
            return new SilenceMessageContent(source);
        }

        @Override
        public SilenceMessageContent[] newArray(int size) {
            return new SilenceMessageContent[size];
        }
    };
}
