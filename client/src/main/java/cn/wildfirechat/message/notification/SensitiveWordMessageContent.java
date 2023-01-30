/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.MESSAGE_SENSITIVE_WORD;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

/**
 * 敏感词 自动屏蔽
 */
@ContentTag(type = MESSAGE_SENSITIVE_WORD, flag = PersistFlag.Persist)
public class SensitiveWordMessageContent extends NotificationMessageContent {
    public SensitiveWordMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
//        显示屏蔽信息memo文字：
//        -自己看到的提示文字：您发送包含敏感词的内容
//        -别人看到的提示文字：<用户名称>发送包含敏感词的内容
        if (fromSelf) {
            return "您发送包含敏感词的内容。";
        } else {
            return "<" + ChatManager.Instance().getUserDisplayName(message.sender) + ">发送包含敏感词的内容。";
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

    protected SensitiveWordMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<SensitiveWordMessageContent> CREATOR = new Creator<SensitiveWordMessageContent>() {
        @Override
        public SensitiveWordMessageContent createFromParcel(Parcel source) {
            return new SensitiveWordMessageContent(source);
        }

        @Override
        public SensitiveWordMessageContent[] newArray(int size) {
            return new SensitiveWordMessageContent[size];
        }
    };
}
