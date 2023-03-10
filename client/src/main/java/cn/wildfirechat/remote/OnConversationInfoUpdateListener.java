/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import cn.wildfirechat.model.ConversationInfo;

public interface OnConversationInfoUpdateListener {

    void onConversationDraftUpdate(ConversationInfo conversationInfo, String draft);

    void onConversationTopUpdate(ConversationInfo conversationInfo, int top);

    void onConversationSilentUpdate(ConversationInfo conversationInfo, boolean silent);

    /**
     * @param conversationInfo
     */
    void onConversationUnreadStatusClear(ConversationInfo conversationInfo);

    // 可能是receive、send、recall 触发
    // 有个问题，未读消息计数做不了，还是得send、receive、recall三个回调来做
    //void onConversationLatestMessageUpdate(ConversationInfo conversationInfo, Message message);
}
