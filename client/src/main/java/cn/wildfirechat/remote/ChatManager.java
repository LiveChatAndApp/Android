/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;


import static android.content.Context.BIND_AUTO_CREATE;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.UserSource;
import cn.wildfirechat.client.ClientService;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.client.ICreateChannelCallback;
import cn.wildfirechat.client.ICreateSecretChatCallback;
import cn.wildfirechat.client.IGeneralCallback;
import cn.wildfirechat.client.IGeneralCallback2;
import cn.wildfirechat.client.IGeneralCallbackInt;
import cn.wildfirechat.client.IGetAuthorizedMediaUrlCallback;
import cn.wildfirechat.client.IGetConversationListCallback;
import cn.wildfirechat.client.IGetFileRecordCallback;
import cn.wildfirechat.client.IGetGroupCallback;
import cn.wildfirechat.client.IGetGroupMemberCallback;
import cn.wildfirechat.client.IGetMessageCallback;
import cn.wildfirechat.client.IGetRemoteMessagesCallback;
import cn.wildfirechat.client.IGetUploadUrlCallback;
import cn.wildfirechat.client.IGetUserCallback;
import cn.wildfirechat.client.IOnChannelInfoUpdateListener;
import cn.wildfirechat.client.IOnConferenceEventListener;
import cn.wildfirechat.client.IOnConnectToServerListener;
import cn.wildfirechat.client.IOnConnectionStatusChangeListener;
import cn.wildfirechat.client.IOnFriendUpdateListener;
import cn.wildfirechat.client.IOnGroupInfoUpdateListener;
import cn.wildfirechat.client.IOnGroupMembersUpdateListener;
import cn.wildfirechat.client.IOnReceiveMessageListener;
import cn.wildfirechat.client.IOnSecretChatStateListener;
import cn.wildfirechat.client.IOnSecretMessageBurnStateListener;
import cn.wildfirechat.client.IOnSettingUpdateListener;
import cn.wildfirechat.client.IOnTrafficDataListener;
import cn.wildfirechat.client.IOnUserInfoUpdateListener;
import cn.wildfirechat.client.IOnUserOnlineEventListener;
import cn.wildfirechat.client.IRemoteClient;
import cn.wildfirechat.client.IUploadMediaCallback;
import cn.wildfirechat.client.IWatchUserOnlineStateCallback;
import cn.wildfirechat.client.NotInitializedExecption;
import cn.wildfirechat.message.ArticlesMessageContent;
import cn.wildfirechat.message.CallStartMessageContent;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.message.ChannelMenuEventMessageContent;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.message.EnterChannelChatMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.JoinCallRequestMessageContent;
import cn.wildfirechat.message.LeaveChannelChatMessageContent;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.LocationMessageContent;
import cn.wildfirechat.message.MarkUnreadMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.message.MultiCallOngoingMessageContent;
import cn.wildfirechat.message.PTTSoundMessageContent;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StickerMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.UnknownMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.AddGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupNameNotificationContent;
import cn.wildfirechat.message.notification.ChangeGroupPortraitNotificationContent;
import cn.wildfirechat.message.notification.CreateGroupNotificationContent;
import cn.wildfirechat.message.notification.DeleteMessageContent;
import cn.wildfirechat.message.notification.DismissGroupNotificationContent;
import cn.wildfirechat.message.notification.FriendAddedMessageContent;
import cn.wildfirechat.message.notification.FriendGreetingMessageContent;
import cn.wildfirechat.message.notification.GroupAllowMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupJoinTypeNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteMemberNotificationContent;
import cn.wildfirechat.message.notification.GroupMuteNotificationContent;
import cn.wildfirechat.message.notification.GroupPrivateChatNotificationContent;
import cn.wildfirechat.message.notification.GroupSetManagerNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberNotificationContent;
import cn.wildfirechat.message.notification.KickoffGroupMemberVisibleNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupAliasNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupExtraNotificationContent;
import cn.wildfirechat.message.notification.ModifyGroupMemberExtraNotificationContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.message.notification.PCLoginRequestMessageContent;
import cn.wildfirechat.message.notification.QuitGroupNotificationContent;
import cn.wildfirechat.message.notification.QuitGroupVisibleNotificationContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.message.notification.RichNotificationMessageContent;
import cn.wildfirechat.message.notification.SensitiveWordMessageContent;
import cn.wildfirechat.message.notification.SilenceMessageContent;
import cn.wildfirechat.message.notification.StartSecretChatMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.message.notification.TransferGroupOwnerNotificationContent;
import cn.wildfirechat.model.BurnMessageInfo;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.ChatRoomMembersInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.CustomChatRoomInfo;
import cn.wildfirechat.model.FileRecord;
import cn.wildfirechat.model.FileRecordOrder;
import cn.wildfirechat.model.Friend;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.ModifyChannelInfoType;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullChannelInfo;
import cn.wildfirechat.model.NullConversationInfo;
import cn.wildfirechat.model.NullGroupInfo;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.ReadEntry;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.Socks5ProxyInfo;
import cn.wildfirechat.model.UnreadCount;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.utils.MemoryFileUtil;

/**
 * Created by WF Chat on 2017/12/11.
 */

public class ChatManager {
    private static final String TAG = "ClientService";

    private String SERVER_HOST;

    private static IRemoteClient mClient;

    private static ChatManager INST;
    private static Context gContext;

    private String userId;
    private String token;
    private Handler mainHandler;
    private Handler workHandler;
    private String deviceToken;
    private String clientId;
    private int pushType;
    private Map<Integer, Class<? extends MessageContent>> messageContentMap = new HashMap<>();
    private boolean isLiteMode = false;
    private boolean isLowBPSMode = false;
    private UserSource userSource;

    private boolean startLog;
    private String sendLogCommand;
    private int connectionStatus;
    private int receiptStatus = -1; // 1, enable
    private int userReceiptStatus = -1; //1, enable

    private int backupAddressStrategy = 1;
    private String backupAddressHost = null;
    private int backupAddressPort = 80;
    private String protoUserAgent = null;
    private Map<String, String> protoHttpHeaderMap = new ConcurrentHashMap<>();

    private boolean useSM4 = false;
    private boolean defaultSilentWhenPCOnline = true;

    private Socks5ProxyInfo proxyInfo;

    private boolean isBackground = true;
    private List<OnReceiveMessageListener> onReceiveMessageListeners = new ArrayList<>();
    private List<OnConnectionStatusChangeListener> onConnectionStatusChangeListeners = new ArrayList<>();
    private List<OnTrafficDataListener> onTrafficDataListeners = new ArrayList<>();
    private List<OnConnectToServerListener> onConnectToServerListeners = new ArrayList<>();
    private List<OnSendMessageListener> sendMessageListeners = new ArrayList<>();
    private List<OnGroupInfoUpdateListener> groupInfoUpdateListeners = new ArrayList<>();
    private List<OnGroupMembersUpdateListener> groupMembersUpdateListeners = new ArrayList<>();
    private List<OnUserInfoUpdateListener> userInfoUpdateListeners = new ArrayList<>();
    private List<OnSettingUpdateListener> settingUpdateListeners = new ArrayList<>();
    private List<OnFriendUpdateListener> friendUpdateListeners = new ArrayList<>();
    private List<OnConversationInfoUpdateListener> conversationInfoUpdateListeners = new ArrayList<>();
    private List<OnRecallMessageListener> recallMessageListeners = new ArrayList<>();
    private List<OnDeleteMessageListener> deleteMessageListeners = new ArrayList<>();
    private List<OnChannelInfoUpdateListener> channelInfoUpdateListeners = new ArrayList<>();
    private List<OnMessageUpdateListener> messageUpdateListeners = new ArrayList<>();
    private List<OnClearMessageListener> clearMessageListeners = new ArrayList<>();
    private List<OnRemoveConversationListener> removeConversationListeners = new ArrayList<>();

    private List<IMServiceStatusListener> imServiceStatusListeners = new ArrayList<>();
    private List<OnMessageDeliverListener> messageDeliverListeners = new ArrayList<>();
    private List<OnMessageReadListener> messageReadListeners = new ArrayList<>();
    private List<OnConferenceEventListener> conferenceEventListeners = new ArrayList<>();
    private List<OnUserOnlineEventListener> userOnlineEventListeners = new ArrayList<>();
    private List<SecretChatStateChangeListener> secretChatStateChangeListeners = new ArrayList<>();
    private List<SecretMessageBurnStateListener> secretMessageBurnStateListeners = new ArrayList<>();


    // key = userId
    private LruCache<String, UserInfo> userInfoCache;
    // key = memberId@groupId
    private LruCache<String, GroupMember> groupMemberCache;
    // key = chatRoomId
    private LruCache<String, CustomChatRoomInfo> chatroomCache;

    private Map<String, UserOnlineState> userOnlineStateMap;

    public enum SearchUserType {
        //????????????displayName???????????????name???????????????
        General(0),

        //????????????name???????????????
        NameOrMobile(1),

        //????????????name
        Name(2),

        //????????????????????????
        Mobile(3);

        private int value;

        SearchUserType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SearchUserType type(int type) {
            SearchUserType searchUserType = null;
            switch (type) {
                case 0:
                    searchUserType = General;
                    break;
                case 1:
                    searchUserType = NameOrMobile;
                    break;
                case 2:
                    searchUserType = Name;
                    break;
                case 3:
                    searchUserType = Mobile;
                    break;
                default:
                    throw new IllegalArgumentException("type " + searchUserType + " is invalid");
            }
            return searchUserType;
        }
    }


    public enum SecretChatState {
        //?????????????????????
        Starting(0),

        //?????????????????????
        Accepting(1),

        //?????????????????????
        Established(2),

        //?????????????????????
        Canceled(3);

        private int value;

        SecretChatState(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static SecretChatState fromValue(int value) {
            for (SecretChatState secretChatState : SecretChatState.values()) {
                if (secretChatState.value == value)
                    return secretChatState;
            }
            return Canceled;
        }
    }

    /**
     * ?????????????????????id
     *
     * @return ??????id
     */
    public String getUserId() {
        return userId;
    }

    public interface IGeneralCallback3 {
        void onSuccess(List<String> results);

        void onFailure(int errorCode);
    }

    private ChatManager(String serverHost) {
        this.SERVER_HOST = serverHost;
    }

    public static ChatManager Instance() throws NotInitializedExecption {
        if (INST == null) {
            throw new NotInitializedExecption();
        }
        return INST;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * serverHost?????????IP???????????????????????????????????????????????????????????????www?????????????????????????????????
     * ?????????example.com???www.example.com???????????????xx.example.com???xx.yy.example.com??????????????????
     *
     * @param context
     * @param imServerHost im server????????????ip
     * @return
     */

    public static void init(Application context, String imServerHost) {
        Log.d(TAG, "init imHost = " + imServerHost);
        if (imServerHost != null) {
            checkSDKHost(imServerHost);
        }
        if (INST != null) {
            // TODO: Already initialized
            return;
        }
//        if (TextUtils.isEmpty(imServerHost)) {
//            throw new IllegalArgumentException("imServerHost must be empty");
//        }
        gContext = context.getApplicationContext();
        INST = new ChatManager(imServerHost);
        INST.mainHandler = new Handler();
        INST.userInfoCache = new LruCache<>(1024);
        INST.groupMemberCache = new LruCache<>(1024);
        INST.chatroomCache = new LruCache<>(1024);
        INST.userOnlineStateMap = new HashMap<>();
        HandlerThread thread = new HandlerThread("workHandler");
        thread.start();
        INST.workHandler = new Handler(thread.getLooper());
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onForeground() {
                INST.isBackground = false;
                if (mClient == null) {
                    return;
                }
                try {
                    mClient.setForeground(1);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onBackground() {
                INST.isBackground = true;
                if (mClient == null) {
                    return;
                }
                try {
                    mClient.setForeground(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        INST.checkRemoteService();

        INST.cleanLogFiles();
        INST.registerCoreMessageContents();
    }

    public Context getApplicationContext() {
        return gContext;
    }

    /**
     * ??????????????????????????????????????????????????????IM???????????????????????????????????????????????????????????????????????????
     *
     * @param userSource ???????????????
     */
    public void setUserSource(UserSource userSource) {
        this.userSource = userSource;
    }

    /**
     * ???????????????????????????
     *
     * @return ?????????????????????{@link cn.wildfirechat.client.ConnectionStatus}
     */
    public int getConnectionStatus() {
        return connectionStatus;
    }

    //App???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    public void forceConnect() {
        if (mClient != null) {
            try {
                mClient.setForeground(1);
                if (INST.isBackground) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mClient != null) {
                                try {
                                    mClient.setForeground(INST.isBackground ? 1 : 0);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, 3000);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param status ????????????
     */
    private void onConnectionStatusChange(final int status) {
        Log.d(TAG, "connectionStatusChange " + status);
        if (status == ConnectionStatus.ConnectionStatusTokenIncorrect || status == ConnectionStatus.ConnectionStatusSecretKeyMismatch) {
            // TODO
            Log.d(TAG, "???????????????????????????" + "https://docs.wildfirechat.cn/faq/general.html");
        }

        //?????????????????????Manager?????????????????????
        if (status == ConnectionStatus.ConnectionStatusConnected) {
            receiptStatus = -1;
            userReceiptStatus = -1;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                connectionStatus = status;
                Iterator<OnConnectionStatusChangeListener> iterator = onConnectionStatusChangeListeners.iterator();
                OnConnectionStatusChangeListener listener;
                while (iterator.hasNext()) {
                    listener = iterator.next();
                    listener.onConnectionStatusChange(status);
                }
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param host ?????????host
     * @param ip   ?????????ip
     * @param port ?????????port
     */
    private void onConnectToServer(final String host, final String ip, final int port) {
        Log.e(TAG, "connectToServer host " + host + ", ip " + ip + ", port " + port);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Iterator<OnConnectToServerListener> iterator = onConnectToServerListeners.iterator();
                OnConnectToServerListener listener;
                while (iterator.hasNext()) {
                    listener = iterator.next();
                    listener.onConnectToServer(host, ip, port);
                }
            }
        });
    }

    /**
     * ???????????????
     *
     * @param messageUid
     */
    private void onRecallMessage(final long messageUid) {
        Message message = getMessageByUid(messageUid);
        // ?????????????????????????????????
        if (message == null) {
            message = new Message();
            // ??????????????????
            message.messageUid = messageUid;
        }
        Message finalMessage = message;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnRecallMessageListener listener : recallMessageListeners) {
                    listener.onRecallMessage(finalMessage);
                }
            }
        });
    }

    /**
     * ???????????????server api ??????
     *
     * @param messageUid
     */
    private void onDeleteMessage(final long messageUid) {
        Message message = new Message();
        message.messageUid = messageUid;
        mainHandler.post(() -> {
            for (OnDeleteMessageListener listener : deleteMessageListeners) {
                listener.onDeleteMessage(message);
            }
        });
    }

    /**
     * ???????????????
     *
     * @param messages
     * @param hasMore  ?????????????????????????????????
     */
    private void onReceiveMessage(final List<Message> messages, final boolean hasMore) {
        mainHandler.post(() -> {
            Iterator<OnReceiveMessageListener> iterator = onReceiveMessageListeners.iterator();
            OnReceiveMessageListener listener;
            while (iterator.hasNext()) {
                listener = iterator.next();
                listener.onReceiveMessage(messages, hasMore);
            }

            // ????????????????????????????????????????????????????????????????????????
            if (messages.size() > 10) {
                return;
            }
            for (Message message : messages) {
                if ((message.content instanceof QuitGroupNotificationContent && ((QuitGroupNotificationContent) message.content).operator.equals(getUserId()))
                        || (message.content instanceof KickoffGroupMemberNotificationContent && ((KickoffGroupMemberNotificationContent) message.content).kickedMembers.contains(getUserId()))
                        || message.content instanceof DismissGroupNotificationContent) {
                    for (OnRemoveConversationListener l : removeConversationListeners) {
                        l.onConversationRemove(message.conversation);
                    }
                }
            }
        });
    }

    private void onMsgDelivered(Map<String, Long> deliveries) {
        mainHandler.post(() -> {
            if (messageDeliverListeners != null) {
                for (OnMessageDeliverListener listener : messageDeliverListeners) {
                    listener.onMessageDelivered(deliveries);
                }
            }
        });
    }

    private void onMsgReaded(List<ReadEntry> readEntries) {
        mainHandler.post(() -> {
            if (messageReadListeners != null) {
                for (OnMessageReadListener listener : messageReadListeners) {
                    listener.onMessageRead(readEntries);
                }
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param userInfos
     */
    private void onUserInfoUpdate(List<UserInfo> userInfos) {
        Log.e(TAG, "onUserInfoUpdate");
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }
        for (UserInfo info : userInfos) {
            userInfoCache.put(info.uid, info);
        }
        mainHandler.post(() -> {
            for (OnUserInfoUpdateListener listener : userInfoUpdateListeners) {
                listener.onUserInfoUpdate(userInfos);
            }
        });
    }

    /**
     * ???????????????
     *
     * @param groupInfos
     */
    private void onGroupInfoUpdated(List<GroupInfo> groupInfos) {
        if (groupInfos == null || groupInfos.isEmpty()) {
            return;
        }
        mainHandler.post(() -> {
            for (OnGroupInfoUpdateListener listener : groupInfoUpdateListeners) {
                listener.onGroupInfoUpdate(groupInfos);
            }

        });
    }

    /**
     * ?????????????????????
     *
     * @param groupId
     * @param groupMembers
     */
    private void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers) {
        if (groupMembers == null || groupMembers.isEmpty()) {
            return;
        }
        for (GroupMember member : groupMembers) {
            groupMemberCache.remove(groupMemberCacheKey(groupId, member.memberId));
        }
        mainHandler.post(() -> {
            for (OnGroupMembersUpdateListener listener : groupMembersUpdateListeners) {
                listener.onGroupMembersUpdate(groupId, groupMembers);
            }
        });
    }

    private void onFriendListUpdated(List<String> friendList) {
        mainHandler.post(() -> {
            for (OnFriendUpdateListener listener : friendUpdateListeners) {
                listener.onFriendListUpdate(friendList);
            }
        });
        onUserInfoUpdate(getUserInfos(friendList, null));
    }

    private void onFriendReqeustUpdated(List<String> newRequests) {
        mainHandler.post(() -> {
            for (OnFriendUpdateListener listener : friendUpdateListeners) {
                listener.onFriendRequestUpdate(newRequests);
            }
        });
    }

    private void onSettingUpdated() {
        mainHandler.post(() -> {
            for (OnSettingUpdateListener listener : settingUpdateListeners) {
                listener.onSettingUpdate();
            }
        });
    }

    private void onChannelInfoUpdate(List<ChannelInfo> channelInfos) {
        mainHandler.post(() -> {
            for (OnChannelInfoUpdateListener listener : channelInfoUpdateListeners) {
                listener.onChannelInfoUpdate(channelInfos);
            }
        });
    }

    private void onConferenceEvent(String event) {
        mainHandler.post(() -> {
            for (OnConferenceEventListener listener : conferenceEventListeners) {
                listener.onConferenceEvent(event);
            }
        });
    }

    private void onUserOnlineEvent(UserOnlineState[] userOnlineStates) {
        mainHandler.post(() -> {
            for (UserOnlineState userOnlineState : userOnlineStates) {
                userOnlineStateMap.put(userOnlineState.getUserId(), userOnlineState);
            }

            for (OnUserOnlineEventListener listener : userOnlineEventListeners) {
                listener.onUserOnlineEvent(userOnlineStateMap);
            }
        });
    }

    private void onSecretChatStateChanged(String targetId, int state) {
        mainHandler.post(() -> {
            for (SecretChatStateChangeListener listener : secretChatStateChangeListeners) {
                listener.onSecretChatStateChanged(targetId, SecretChatState.fromValue(state));
            }
        });
    }

    private void onSecretMessageStartBurning(String targetId, long playedMsgId) {
        mainHandler.post(() -> {
            for (SecretMessageBurnStateListener listener : secretMessageBurnStateListeners) {
                listener.onSecretMessageStartBurning(targetId, playedMsgId);
            }
        });
    }

    private void onSecretMessageBurned(List<Long> messageIds) {
        mainHandler.post(() -> {
            for (SecretMessageBurnStateListener listener : secretMessageBurnStateListeners) {
                listener.onSecretMessageBurned(messageIds);
            }
        });
    }

    public UserOnlineState getUserOnlineState(String userId) {
        return userOnlineStateMap.get(userId);
    }

    public Map<String, UserOnlineState> getUserOnlineStateMap() {
        return userOnlineStateMap;
    }

    private void onTrafficData(long send, long recv) {
        mainHandler.post(() -> {
            for (OnTrafficDataListener listener : onTrafficDataListeners) {
                listener.onTrafficData(send, recv);
            }
        });
    }

    public void watchOnlineState(int conversationType, String[] targets, int duration, WatchOnlineStateCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.watchUserOnlineState(conversationType, targets, duration, new IWatchUserOnlineStateCallback.Stub() {
                @Override
                public void onSuccess(UserOnlineState[] states) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            for (UserOnlineState state : states) {
                                userOnlineStateMap.put(state.getUserId(), state);
                            }
                            callback.onSuccess(states);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void unWatchOnlineState(int conversationType, String[] targets, GeneralCallback callback) {
        try {
            mClient.unwatchOnlineState(conversationType, targets, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(callback::onSuccess);
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * ?????????????????????, ????????????{@link #removeOnReceiveMessageListener(OnReceiveMessageListener)}????????????
     *
     * @param listener
     */
    public void addOnReceiveMessageListener(OnReceiveMessageListener listener) {
        if (listener == null) {
            return;
        }
        onReceiveMessageListeners.add((listener));
    }

    /**
     * ??????????????????
     *
     * @param listener
     */
    public void removeOnReceiveMessageListener(OnReceiveMessageListener listener) {
        if (listener == null) {
            return;
        }
        onReceiveMessageListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addSendMessageListener(OnSendMessageListener listener) {
        if (listener == null) {
            return;
        }
        sendMessageListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeSendMessageListener(OnSendMessageListener listener) {
        sendMessageListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addConnectionChangeListener(OnConnectionStatusChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (!onConnectionStatusChangeListeners.contains(listener)) {
            onConnectionStatusChangeListeners.add(listener);
        }
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeConnectionChangeListener(OnConnectionStatusChangeListener listener) {
        if (listener == null) {
            return;
        }
        onConnectionStatusChangeListeners.remove(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void addConnectToServerListener(OnConnectToServerListener listener) {
        if (listener == null) {
            return;
        }
        if (!onConnectToServerListeners.contains(listener)) {
            onConnectToServerListeners.add(listener);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void removeConnectToServerListener(OnConnectToServerListener listener) {
        if (listener == null) {
            return;
        }
        onConnectToServerListeners.remove(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void addGroupInfoUpdateListener(OnGroupInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        groupInfoUpdateListeners.add(listener);
    }

    /**
     * ?????????????????????
     *
     * @param listener
     */
    public void removeGroupInfoUpdateListener(OnGroupInfoUpdateListener listener) {
        groupInfoUpdateListeners.remove(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void addGroupMembersUpdateListener(OnGroupMembersUpdateListener listener) {
        if (listener != null) {
            groupMembersUpdateListeners.add(listener);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void removeGroupMembersUpdateListener(OnGroupMembersUpdateListener listener) {
        groupMembersUpdateListeners.remove(listener);
    }

    /**
     * ??????????????????????????????
     *
     * @param listener
     */
    public void addUserInfoUpdateListener(OnUserInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        userInfoUpdateListeners.add(listener);
    }

    /**
     * ??????????????????????????????
     *
     * @param listener
     */
    public void removeUserInfoUpdateListener(OnUserInfoUpdateListener listener) {
        userInfoUpdateListeners.remove(listener);
    }

    /**
     * ??????????????????????????????
     *
     * @param listener
     */
    public void addFriendUpdateListener(OnFriendUpdateListener listener) {
        if (listener == null) {
            return;
        }
        friendUpdateListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeFriendUpdateListener(OnFriendUpdateListener listener) {
        friendUpdateListeners.remove(listener);
    }

    /**
     * ??????????????????????????????
     *
     * @param listener
     */
    public void addSettingUpdateListener(OnSettingUpdateListener listener) {
        if (listener == null) {
            return;
        }
        settingUpdateListeners.add(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void removeSettingUpdateListener(OnSettingUpdateListener listener) {
        settingUpdateListeners.remove(listener);
    }

    /**
     * ??????????????????
     *
     * @param listener
     */
    public void addTrafficDataListener(OnTrafficDataListener listener) {
        if (listener == null) {
            return;
        }
        if (!onTrafficDataListeners.contains(listener)) {
            onTrafficDataListeners.add(listener);
        }
    }

    /**
     * ??????????????????
     *
     * @param listener
     */
    public void removeTrafficDataListener(OnTrafficDataListener listener) {
        if (listener == null) {
            return;
        }
        onTrafficDataListeners.remove(listener);
    }


    /**
     * ??????????????????????????????connect?????????????????????IM????????????????????????????????????
     */
    public void useSM4() {
        useSM4 = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.useSM4();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????clientId, ??????IM???clientId????????????????????????
     */
    public synchronized String getClientId() {
        if (this.clientId != null) {
            return this.clientId;
        }

        String imei = null;
        try (
                RandomAccessFile fw = new RandomAccessFile(gContext.getFilesDir().getAbsoluteFile() + "/.wfcClientId", "rw");
                FileChannel chan = fw.getChannel();
        ) {
            FileLock lock = chan.lock();
            imei = fw.readLine();
            if (TextUtils.isEmpty(imei)) {
                //  ????????????clientId
                imei = PreferenceManager.getDefaultSharedPreferences(gContext).getString("mars_core_uid", "");
                if (TextUtils.isEmpty(imei)) {
                    if (TextUtils.isEmpty(imei)) {
                        imei = UUID.randomUUID().toString();
                    }
                    imei += System.currentTimeMillis();
                }
                fw.writeBytes(imei);
            }
            lock.release();
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("getClientError", "" + ex.getMessage());
        }
        this.clientId = imei;
        Log.d(TAG, "clientId " + this.clientId);
        return imei;
    }

    /**
     * ????????????
     *
     * @param channelId       ??????id????????????null????????????????????????id?????????????????????????????????id??????????????????id????????????
     * @param channelName     ????????????
     * @param channelPortrait ???????????????????????????
     * @param desc            ????????????
     * @param extra           ??????????????????????????????????????????????????????
     * @param callback        ???????????????????????????
     */
    public void createChannel(@Nullable String channelId, String channelName, String channelPortrait, String desc, String extra, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.createChannel(channelId, channelName, channelPortrait, desc, extra, new ICreateChannelCallback.Stub() {
                @Override
                public void onSuccess(ChannelInfo channelInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(channelInfo.channelId));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param channelId  ??????id
     * @param modifyType ????????????????????????????????????????????????
     * @param newValue   ???????????????
     * @param callback   ??????????????????
     */
    public void modifyChannelInfo(String channelId, ModifyChannelInfoType modifyType, String newValue, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.modifyChannelInfo(channelId, modifyType.ordinal(), newValue, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }


    /**
     * ??????????????????
     *
     * @param channelId
     * @param refresh   ??????????????????????????????true?????????????????????????????????????????????????????????????????????????????????{@link OnChannelInfoUpdateListener}????????????????????????
     * @return ????????????????????????null
     */
    public @Nullable
    ChannelInfo getChannelInfo(String channelId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullChannelInfo(channelId);
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            return new NullChannelInfo(channelId);
        }

        try {
            ChannelInfo channelInfo = mClient.getChannelInfo(channelId, refresh);
            if (channelInfo == null) {
                channelInfo = new NullChannelInfo(channelId);
            }
            return channelInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param keyword  ???????????????
     * @param callback ??????????????????
     */
    public void searchChannel(String keyword, SearchChannelCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(keyword)) {
            Log.e(TAG, "Error, keyword is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.searchChannel(keyword, new cn.wildfirechat.client.ISearchChannelCallback.Stub() {

                @Override
                public void onSuccess(final List<ChannelInfo> channelInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(channelInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param channelId
     * @return true, ????????????false????????????
     */
    public boolean isListenedChannel(String channelId) {
        if (!checkRemoteService()) {
            return false;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            return false;
        }

        try {
            return mClient.isListenedChannel(channelId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * ???????????????????????????
     *
     * @param channelId
     * @param listen    true????????????false???????????????
     * @param callback  ??????????????????
     */
    public void listenChannel(String channelId, boolean listen, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.listenChannel(channelId, listen, new cn.wildfirechat.client.IGeneralCallback.Stub() {

                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param channelId
     * @param callback
     */
    public void destoryChannel(String channelId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(channelId)) {
            Log.e(TAG, "Error, channelId is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.destoryChannel(channelId, new cn.wildfirechat.client.IGeneralCallback.Stub() {

                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????????????????id??????
     *
     * @return
     */
    public List<String> getMyChannels() {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }

        try {
            return mClient.getMyChannels();
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * ????????????????????????id??????
     *
     * @return
     */
    public List<String> getListenedChannels() {
        if (!checkRemoteService()) {
            return new ArrayList<>();
        }

        try {
            return mClient.getListenedChannels();
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * ????????????????????????id??????
     *
     * @return
     */
    public void getRemoteListenedChannels(GeneralCallback3 callback3) {
        if (!checkRemoteService()) {
            callback3.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getRemoteListenedChannels(new cn.wildfirechat.client.IGeneralCallback3.Stub() {
                @Override
                public void onSuccess(List<String> results) throws RemoteException {
                    mainHandler.post(() -> callback3.onSuccess(results));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback3.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            callback3.onFail(ErrorCode.SERVICE_DIED);
        }
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addConversationInfoUpdateListener(OnConversationInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        conversationInfoUpdateListeners.add(listener);
    }

    /**
     * ??????????????????
     *
     * @param listener
     */
    public void removeConversationInfoUpdateListener(OnConversationInfoUpdateListener listener) {
        conversationInfoUpdateListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addRecallMessageListener(OnRecallMessageListener listener) {
        if (listener == null) {
            return;
        }
        recallMessageListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeRecallMessageListener(OnRecallMessageListener listener) {
        recallMessageListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addDeleteMessageListener(OnDeleteMessageListener listener) {
        if (listener == null) {
            return;
        }
        deleteMessageListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeDeleteMessageListener(OnDeleteMessageListener listener) {
        deleteMessageListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addChannelInfoUpdateListener(OnChannelInfoUpdateListener listener) {
        if (listener == null) {
            return;
        }
        channelInfoUpdateListeners.add(listener);
    }

    /**
     * ??????????????????????????????
     *
     * @param listener
     */
    public void removeChannelInfoListener(OnChannelInfoUpdateListener listener) {
        channelInfoUpdateListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addOnMessageUpdateListener(OnMessageUpdateListener listener) {
        if (listener == null) {
            return;
        }
        messageUpdateListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeOnMessageUpdateListener(OnMessageUpdateListener listener) {
        messageUpdateListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addClearMessageListener(OnClearMessageListener listener) {
        if (listener == null) {
            return;
        }

        clearMessageListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeClearMessageListener(OnClearMessageListener listener) {
        clearMessageListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addRemoveConversationListener(OnRemoveConversationListener listener) {
        if (listener == null) {
            return;
        }
        removeConversationListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeRemoveConversationListener(OnRemoveConversationListener listener) {
        removeConversationListeners.remove(listener);
    }


    /**
     * ??????im????????????????????????
     *
     * @param listener
     */
    public void addIMServiceStatusListener(IMServiceStatusListener listener) {
        if (listener == null) {
            return;
        }
        imServiceStatusListeners.add(listener);
    }

    /**
     * ??????im????????????????????????
     *
     * @param listener
     */
    public void removeIMServiceStatusListener(IMServiceStatusListener listener) {
        imServiceStatusListeners.remove(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void addMessageDeliverListener(OnMessageDeliverListener listener) {
        if (listener == null) {
            return;
        }
        messageDeliverListeners.add(listener);
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     */
    public void removeMessageDeliverListener(OnMessageDeliverListener listener) {
        messageDeliverListeners.remove(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void addMessageReadListener(OnMessageReadListener listener) {
        if (listener == null) {
            return;
        }
        messageReadListeners.add(listener);
    }

    /**
     * ????????????????????????
     *
     * @param listener
     */
    public void removeMessageReadListener(OnMessageReadListener listener) {
        messageReadListeners.remove(listener);
    }

    public void addConferenceEventListener(OnConferenceEventListener listener) {
        if (listener == null) {
            return;
        }
        conferenceEventListeners.add(listener);
    }

    public void removeConferenceEventListener(OnConferenceEventListener listener) {
        conferenceEventListeners.remove(listener);
    }

    public void addUserOnlineEventListener(OnUserOnlineEventListener listener) {
        if (listener == null) {
            return;
        }
        userOnlineEventListeners.add(listener);
    }

    public void removeUserOnlineEventListener(OnUserOnlineEventListener listener) {
        userOnlineEventListeners.remove(listener);
    }

    public void addSecretChatStateChangedListener(SecretChatStateChangeListener listener) {
        if (listener == null) {
            return;
        }
        secretChatStateChangeListeners.add(listener);
    }

    public void removeSecretChatStateChangedListener(SecretChatStateChangeListener listener) {
        secretChatStateChangeListeners.remove(listener);
    }

    public void addSecretMessageBurnStateListener(SecretMessageBurnStateListener listener) {
        if (listener == null) {
            return;
        }
        secretMessageBurnStateListeners.add(listener);
    }

    public void removeSecretMessageBurnStateListener(SecretMessageBurnStateListener listener) {
        secretMessageBurnStateListeners.remove(listener);
    }

    private void validateMessageContent(Class<? extends MessageContent> msgContentClazz) {
        String className = msgContentClazz.getName();
        try {
            Constructor c = msgContentClazz.getConstructor();
            if (c.getModifiers() != Modifier.PUBLIC) {
                throw new IllegalArgumentException(className + ", the default constructor of your custom messageContent class should be public??????????????????????????????????????????public???????????????TextMessageContent.java");
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class must have a default constructor???????????????????????????????????????????????????????????????????????????TextMessageContent.java");
        }

        // ???????????????????????????????????????????????????????????????
//        try {
//            msgContentClazz.getDeclaredMethod("encode");
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            throw new IllegalArgumentException(className + ", custom messageContent class must override encode??????????????????????????????encode??????????????????super.encode()????????????TextMessageContent.java");
//        }

        try {
            Field creator = msgContentClazz.getDeclaredField("CREATOR");
            if ((creator.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC)) == 0) {
                throw new IllegalArgumentException(className + ", custom messageContent class implements Parcelable but does not provide a CREATOR field??????????????????????????????Parcelable????????????????????????CREATOR????????????TextMessageContent.java");
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class implements Parcelable but does not provide a CREATOR field??????????????????????????????Parcelable???????????????????????????CREATOR????????????TextMessageContent.java");
        }

        try {
            msgContentClazz.getDeclaredMethod("writeToParcel", Parcel.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(className + ", custom messageContent class must override writeToParcel??????????????????????????????writeToParcel??????????????????TextMessageContent.java");
        }

        ContentTag tag = msgContentClazz.getAnnotation(ContentTag.class);
        if (tag == null) {
            throw new IllegalArgumentException(className + ", custom messageContent class must have a ContentTag annotation?????????????????????????????????ContentTag??????????????????TextMessageContent.java");
        }

        if (tag.type() == 0 && !msgContentClazz.equals(UnknownMessageContent.class)) {
            throw new IllegalArgumentException(className + ", custom messageContent class's ContentTag annotation must set the type value?????????????????????ContentTag?????????type??????????????????????????????TextMessageContent.java");
        }
    }

    /**
     * ????????????????????????
     *
     * @param msgContentCls ?????????????????????????????????????????????????????????
     */
    public void registerMessageContent(Class<? extends MessageContent> msgContentCls) {

        validateMessageContent(msgContentCls);
        ContentTag tag = (ContentTag) msgContentCls.getAnnotation(ContentTag.class);
        messageContentMap.put(tag.type(), msgContentCls);
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.registerMessageContent(msgContentCls.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????????????????
     * ??? uniapp ??????????????????
     *
     * @param type ????????????
     * @param flag ??????????????????
     */
    public void registerMessageFlag(int type, PersistFlag flag) {
        try {
            mClient.registerMessageFlag(type, flag.getValue());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param conversation ????????????
     * @param sender       ???????????????id
     * @param content      ?????????
     * @param status       ????????????
     * @param notify       ??????????????????????????????????????????{@link #onReceiveMessage(List, boolean)}????????????
     * @param serverTime   ???????????????
     * @return
     */
    public Message insertMessage(Conversation conversation, String sender, MessageContent content, MessageStatus status, boolean notify, long serverTime) {
        return insertMessage(conversation, sender, 0, content, status, notify, serverTime);
    }

    /**
     * ????????????
     *
     * @param conversation ????????????
     * @param sender       ???????????????id
     * @param messageUid   ??????uid????????????0
     * @param content      ?????????
     * @param status       ????????????
     * @param notify       ??????????????????????????????????????????{@link #onReceiveMessage(List, boolean)}????????????
     * @param serverTime   ???????????????
     * @return
     */
    public Message insertMessage(Conversation conversation, String sender, long messageUid, MessageContent content, MessageStatus status, boolean notify, long serverTime) {
        if (!checkRemoteService()) {
            return null;
        }

        Message message = new Message();
        message.conversation = conversation;
        message.content = content;
        message.status = status;
        message.messageUid = messageUid;
        message.serverTime = serverTime;

        message.direction = MessageDirection.Send;
        if (status.value() >= MessageStatus.Mentioned.value()) {
            message.direction = MessageDirection.Receive;
            if (conversation.type == Conversation.ConversationType.Single) {
                message.sender = conversation.target;
            } else {
                message.sender = sender;
            }
        } else {
            message.sender = getUserId();
        }

        try {
            message = mClient.insertMessage(message, notify);
            if (notify) {
                onReceiveMessage(Collections.singletonList(message), false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }

        return message;
    }

    /**
     * ??????????????????
     *
     * @param messageId     ??????id
     * @param newMsgContent ???????????????????????????????????????????????????
     * @return
     */
    public boolean updateMessage(long messageId, MessageContent newMsgContent) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }
            message.content = newMsgContent;
            boolean result = mClient.updateMessageContent(message);
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????????
     *
     * @param messageId     ??????id
     * @param newMsgContent ???????????????????????????????????????????????????
     * @param timestamp     ?????????
     * @return
     */
    public boolean updateMessage(long messageId, MessageContent newMsgContent, long timestamp) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }
            message.content = newMsgContent;
            message.serverTime = timestamp;

            boolean result = mClient.updateMessageContentAndTime(message);
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param messageId ??????id
     * @param status    ???????????????????????????????????????????????????
     * @return
     */
    public boolean updateMessage(long messageId, MessageStatus status) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            Message message = mClient.getMessage(messageId);
            message.status = status;
            if (message == null) {
                Log.e(TAG, "update message failure, message not exist");
                return false;
            }

//            if ((message.direction == MessageDirection.Send && status.value() >= MessageStatus.Mentioned.value()) ||
//                    message.direction == MessageDirection.Receive && status.value() < MessageStatus.Mentioned.value()) {
//                return false;
//            }

            boolean result = mClient.updateMessageStatus(messageId, status.value());
            mainHandler.post(() -> {
                for (OnMessageUpdateListener listener : messageUpdateListeners) {
                    listener.onMessageUpdate(message);
                }
            });
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ??????Lite?????????
     * <p>
     * Lite??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????connect???????????????
     *
     * @param isLiteMode ?????????Lite??????
     */
    public void setLiteMode(boolean isLiteMode) {
        this.isLiteMode = isLiteMode;
        if (mClient != null) {
            try {
                mClient.setLiteMode(isLiteMode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ?????????????????????
     * <p>
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????connect???????????????
     *
     * @param isLowBPSMode ?????????????????????
     */
    public void setLowBPSMode(boolean isLowBPSMode) {
        this.isLowBPSMode = isLowBPSMode;
        if (mClient != null) {
            try {
                mClient.setLowBPSMode(isLowBPSMode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ???????????????
     * userId???token??????????????????
     * ????????????token???clientId?????????????????????????????????getClientId?????????clientId??????????????????clientId??????token?????????connect???????????????????????????????????????clientId????????????token????????????????????????
     * ??????????????????connect?????????????????????????????????disconnect?????????3???????????????connect????????????????????????????????????????????????????????????????????????3??????????????????????????????????????????3??????
     *
     * @param userId
     * @param token
     * @return ?????????????????????????????????????????????????????????????????????????????????????????????
     */
    public boolean connect(String userId, String token) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(token) || TextUtils.isEmpty(SERVER_HOST)) {
            throw new IllegalArgumentException("userId, token and im_server_host must not be empty!");
        }
        this.userId = userId;
        this.token = token;
        Log.e(TAG, "connect imHost " + SERVER_HOST);
        if (mClient != null) {
            try {
                Log.e(TAG, "connect id: " + userId + " token: " + token);
                return mClient.connect(this.userId, this.token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Mars service not start yet!");
        }
        return false;
    }

    /**
     * ??????????????????
     *
     * @param disablePush  ????????????????????????cleanSession???true????????????
     * @param cleanSession ??????????????????session?????????????????????????????????????????????????????????
     */
    public void disconnect(boolean disablePush, boolean cleanSession) {
        if (mClient != null) {
            try {
                Log.d(TAG, "disconnect " + disablePush + " " + cleanSession);
                mClient.disconnect(disablePush, cleanSession);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            this.userId = null;
            this.token = null;
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param strategy ???????????????0??????????????????1????????????????????????2??????????????????
     */
    public void setBackupAddressStrategy(int strategy) {
        backupAddressStrategy = strategy;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setBackupAddressStrategy(strategy);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setIMServerHost(String imServerHost) {
        SERVER_HOST = imServerHost;
        if (mClient != null) {
            try {
                mClient.setServerAddress(imServerHost);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param host ???????????????ip
     * @param port ?????????????????????
     */
    public void setBackupAddress(String host, int port) {
        backupAddressHost = host;
        backupAddressPort = port;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setBackupAddress(host, port);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????UA???
     *
     * @param userAgent ???????????????????????????UA
     */
    public void setProtoUserAgent(String userAgent) {
        protoUserAgent = userAgent;

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setProtoUserAgent(userAgent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????Header
     *
     * @param header ???????????????????????????UA
     * @param value  ???????????????????????????UA
     */
    public void addHttpHeader(String header, String value) {
        if (!TextUtils.isEmpty(value)) {
            protoHttpHeaderMap.put(header, value);
        }

        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.addHttpHeader(header, value);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????
     *
     * @param msg
     * @param expireDuration
     * @param callback
     */
    public void sendSavedMessage(Message msg, int expireDuration, SendMessageCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            for (OnSendMessageListener listener : sendMessageListeners) {
                listener.onSendFail(msg, ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.sendSavedMessage(msg, expireDuration, new cn.wildfirechat.client.ISendMessageCallback.Stub() {
                @Override
                public void onSuccess(long messageUid, long timestamp) throws RemoteException {
                    msg.messageUid = messageUid;
                    msg.serverTime = timestamp;
                    msg.status = MessageStatus.Sent;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(messageUid, timestamp);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendSuccess(msg);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    msg.status = MessageStatus.Send_Failure;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFail(errorCode);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendFail(msg, errorCode);
                            }
                        }
                    });
                }

                @Override
                public void onPrepared(final long messageId, final long savedTime) throws RemoteException {
                    msg.messageId = messageId;
                    msg.serverTime = savedTime;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onPrepare(messageId, savedTime);
                        }
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onSendPrepare(msg, savedTime);
                        }
                    });
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(uploaded, total));
                    }

                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onProgress(msg, uploaded, total);
                        }
                    });
                }

                @Override
                public void onMediaUploaded(final String remoteUrl) throws RemoteException {
                    MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                    mediaMessageContent.remoteUrl = remoteUrl;
                    if (msg.messageId == 0) {
                        return;
                    }
                    if (callback != null) {
                        mainHandler.post(() -> callback.onMediaUpload(remoteUrl));
                    }
                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onMediaUpload(msg, remoteUrl);
                        }
                    });
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param conversation
     * @param content
     * @param toUsers        ?????????????????????????????????????????????????????????????????????
     * @param expireDuration
     * @param callback
     */
    public void sendMessage(Conversation conversation, MessageContent content, String[] toUsers, int expireDuration, SendMessageCallback callback) {
        Message msg = new Message();
        msg.conversation = conversation;
        msg.content = content;
        msg.toUsers = toUsers;
        sendMessage(msg, expireDuration, callback);
    }

    /**
     * ????????????
     *
     * @param msg
     * @param callback ????????????????????????
     */
    public void sendMessage(final Message msg, final SendMessageCallback callback) {
        sendMessage(msg, 0, callback);
    }

    /**
     * ????????????
     *
     * @param msg            ??????
     * @param callback       ??????????????????
     * @param expireDuration 0, ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void sendMessage(final Message msg, final int expireDuration, final SendMessageCallback callback) {
        msg.direction = MessageDirection.Send;
        msg.status = MessageStatus.Sending;
        msg.serverTime = System.currentTimeMillis();
        msg.sender = userId;
        if (!checkRemoteService()) {
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            for (OnSendMessageListener listener : sendMessageListeners) {
                listener.onSendFail(msg, ErrorCode.SERVICE_DIED);
            }
            return;
        }

        MediaMessageUploadCallback mediaMessageUploadCallback = null;
        if (msg.content instanceof MediaMessageContent) {
            if (TextUtils.isEmpty(((MediaMessageContent) msg.content).remoteUrl)) {
                String localPath = ((MediaMessageContent) msg.content).localPath;
                if (!TextUtils.isEmpty(localPath)) {
                    File file = new File(localPath);
                    if (!file.exists()) {
                        if (callback != null) {
                            callback.onFail(ErrorCode.FILE_NOT_EXIST);
                        }
                        return;
                    }

                    if (file.length() >= 100 * 1024 * 1024 && (!isSupportBigFilesUpload() || msg.conversation.type == Conversation.ConversationType.SecretChat)) {
                        if (callback != null) {
                            callback.onFail(ErrorCode.FILE_TOO_LARGE);
                        }
                        return;
                    }
                }
            }
        } else if (msg.content instanceof TextMessageContent) {
            if (sendLogCommand != null && sendLogCommand.equals(((TextMessageContent) msg.content).getContent())) {
                List<String> logFilesPath = getLogFilesPath();
                if (logFilesPath.size() > 0) {
                    FileMessageContent fileMessageContent = new FileMessageContent(logFilesPath.get(logFilesPath.size() - 1));
                    msg.content = fileMessageContent;

                    mediaMessageUploadCallback = new MediaMessageUploadCallback() {
                        @Override
                        public void onMediaMessageUploaded(String remoteUrl) {
                            TextMessageContent textMessageContent = new TextMessageContent(remoteUrl);
                            sendMessage(msg.conversation, textMessageContent, null, 0, null);
                        }
                    };
                }
            }
        }

        try {
            MediaMessageUploadCallback finalMediaMessageUploadCallback = mediaMessageUploadCallback;
            mClient.send(msg, new cn.wildfirechat.client.ISendMessageCallback.Stub() {
                @Override
                public void onSuccess(final long messageUid, final long timestamp) throws RemoteException {
                    msg.messageUid = messageUid;
                    msg.serverTime = timestamp;
                    msg.status = MessageStatus.Sent;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(messageUid, timestamp);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendSuccess(msg);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    Log.e(TAG, "send msg error code = " + errorCode);
                    // ????????? ????????????
                    if (errorCode == ErrorCode.SENSITIVE_MATCHED) {
                        msg.status = MessageStatus.Sent;
                        SensitiveWordMessageContent sensitiveWordMessageContent = new SensitiveWordMessageContent();
                        sensitiveWordMessageContent.fromSelf = true;
                        sensitiveWordMessageContent.mentionedType = 0;
                        msg.content = sensitiveWordMessageContent;
                        // ???????????? content
                        mClient.updateMessageContent(msg);
                    } else if (errorCode == ErrorCode.SEND_MESSAGE_SILENCE) {
                        // ?????????????????????????????????30???
                        msg.status = MessageStatus.Sent;
                        SilenceMessageContent silenceMessageContent = new SilenceMessageContent();
                        silenceMessageContent.fromSelf = true;
                        silenceMessageContent.mentionedType = 0;
                        msg.content = silenceMessageContent;
                        // ???????????? content
                        mClient.updateMessageContent(msg);
                    } else {
                        msg.status = MessageStatus.Send_Failure;
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFail(errorCode);
                            }
                            for (OnSendMessageListener listener : sendMessageListeners) {
                                listener.onSendFail(msg, errorCode);
                            }
                        }
                    });
                }

                @Override
                public void onPrepared(final long messageId, final long savedTime) throws RemoteException {
                    msg.messageId = messageId;
                    msg.serverTime = savedTime;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onPrepare(messageId, savedTime);
                        }
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onSendPrepare(msg, savedTime);
                        }
                    });
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onProgress(uploaded, total));
                    }

                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onProgress(msg, uploaded, total);
                        }
                    });
                }

                @Override
                public void onMediaUploaded(final String remoteUrl) throws RemoteException {
                    MediaMessageContent mediaMessageContent = (MediaMessageContent) msg.content;
                    mediaMessageContent.remoteUrl = remoteUrl;
                    if (msg.messageId == 0) {
                        return;
                    }

                    if (finalMediaMessageUploadCallback != null) {
                        finalMediaMessageUploadCallback.onMediaMessageUploaded(remoteUrl);
                    }

                    if (callback != null) {
                        mainHandler.post(() -> callback.onMediaUpload(remoteUrl));
                    }
                    mainHandler.post(() -> {
                        for (OnSendMessageListener listener : sendMessageListeners) {
                            listener.onMediaUpload(msg, remoteUrl);
                        }
                    });
                }
            }, expireDuration);
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                msg.status = MessageStatus.Send_Failure;
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
            mainHandler.post(() -> {
                for (OnSendMessageListener listener : sendMessageListeners) {
                    listener.onSendFail(msg, ErrorCode.SERVICE_EXCEPTION);
                }
            });
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param messageId
     * @return ??????????????????
     */
    public boolean cancelSendingMessage(long messageId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.cancelSendingMessage(messageId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ????????????
     *
     * @param msg      ??????????????????
     * @param callback ????????????
     */
    public void recallMessage(Message msg, final GeneralCallback callback) {
        try {
            mClient.recall(msg.messageUid, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (msg.messageId > 0) {
                        Message recallMsg = mClient.getMessage(msg.messageId);
                        msg.content = recallMsg.content;
                        msg.sender = recallMsg.sender;
                        msg.serverTime = recallMsg.serverTime;
                    } else {
                        MessagePayload payload = msg.content.encode();
                        RecallMessageContent recallCnt = new RecallMessageContent();
                        recallCnt.setOperatorId(userId);
                        recallCnt.setMessageUid(msg.messageUid);
                        recallCnt.fromSelf = true;
                        recallCnt.setOriginalSender(msg.sender);
                        recallCnt.setOriginalContent(payload.content);
                        recallCnt.setOriginalContentType(payload.type);
                        recallCnt.setOriginalExtra(payload.extra);
                        recallCnt.setOriginalSearchableContent(payload.searchableContent);
                        recallCnt.setOriginalMessageTimestamp(msg.serverTime);
                        msg.content = recallCnt;
                        msg.sender = userId;
                        msg.serverTime = System.currentTimeMillis();
                    }

                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        for (OnRecallMessageListener listener : recallMessageListeners) {
                            listener.onRecallMessage(msg);
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        Log.d(TAG, "shutdown");
        if (mClient != null) {
            gContext.unbindService(serviceConnection);
        }
    }

    /**
     * ????????????handler
     *
     * @return
     */
    public Handler getWorkHandler() {
        return workHandler;
    }


    /**
     * ???????????????handler
     *
     * @return
     */
    public Handler getMainHandler() {
        return mainHandler;
    }

    /**
     * ??????????????????
     * <p>
     * ?????? ipc ??????????????????????????????????????????????????? {@link ChatManager#getConversationListAsync}
     *
     * @param conversationTypes ???????????????????????????
     * @param lines             ????????????????????????
     * @return
     */
    @NonNull
    public List<ConversationInfo> getConversationList(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return new ArrayList<>();
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type and lines");
            return new ArrayList<>();
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.getConversationList(intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * ??????????????????
     *
     * @param conversationTypes ???????????????????????????
     * @param lines             ????????????????????????
     * @param callback          ??????????????????
     */
    public void getConversationListAsync(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, GetConversationListCallback callback) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        if (callback == null) {
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type and lines");
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            List<ConversationInfo> convs = new ArrayList<>();
            mClient.getConversationListAsync(intypes, inlines, new IGetConversationListCallback.Stub() {
                @Override
                public void onSuccess(List<ConversationInfo> infos, boolean hasMore) throws RemoteException {
                    convs.addAll(infos);
                    if (!hasMore) {
                        mainHandler.post(() -> {
                            callback.onSuccess(convs);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            callback.onFail(-1);
        }
    }

    /**
     * ??????????????????
     *
     * @param conversation
     * @return
     */
    public @Nullable
    ConversationInfo getConversation(Conversation conversation) {
        ConversationInfo conversationInfo = null;
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        try {
            conversationInfo = mClient.getConversation(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        conversationInfo = conversationInfo != null ? conversationInfo : new NullConversationInfo(conversation);
        return conversationInfo;
    }

    /**
     * ??????????????????
     *
     * @return
     */
    public ConversationInfo getConversation(int type, String targetId, int line) {
        ConversationInfo conversationInfo = null;
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        try {
            conversationInfo = mClient.getConversation(type, targetId, line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return conversationInfo;
    }

    public long getFirstUnreadMessageId(Conversation conversation) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return 0L;
        }

        try {
            return mClient.getFirstUnreadMessageId(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return 0L;
    }

    /**
     * ??????????????????
     *
     * @param conversation ??????
     * @param fromIndex    ????????????id(messageId)
     * @param before       true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count        ??????????????????
     * @param withUser     ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @return ??????ipc???????????????????????????????????????????????????????????????????????????????????????
     */
    @Deprecated
    public List<Message> getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessages(conversation, fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Deprecated()
    public List<Message> getMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0 ||
                contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            return mClient.getMessagesEx(intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Deprecated
    public List<Message> getMessagesEx2(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<MessageStatus> messageStatus, long fromIndex, boolean before, int count, String withUser) {
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            return null;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        int[] status = new int[messageStatus.size()];
        for (int i = 0; i < messageStatus.size(); i++) {
            status[i] = messageStatus.get(i).ordinal();
        }

        try {
            return mClient.getMessagesEx2(intypes, convertIntegers(lines), status, fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ??????????????????
     *
     * @param conversation
     * @param fromIndex    ????????????id(messageId)
     * @param before       true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count        ??????????????????
     * @param withUser     ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback     ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessages(Conversation conversation, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getMessagesAsync(conversation, fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    outMsgs.addAll(messages);
                    if (!hasMore) {
                        mainHandler.post(() -> callback.onSuccess(outMsgs, false));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????
     *
     * @param conversation ??????
     * @param contentTypes ??????????????????
     * @param fromIndex    ????????????id(messageId)
     * @param before       true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count        ??????????????????
     * @param withUser     ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback     ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessages(Conversation conversation, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getMessagesInTypesAsync(conversation, convertIntegers(contentTypes), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    outMsgs.addAll(messages);
                    if (!hasMore) {
                        mainHandler.post(() -> callback.onSuccess(outMsgs, false));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param conversation  ??????
     * @param messageStatus ??????????????????
     * @param fromIndex     ????????????id(messageId)
     * @param before        true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count         ??????????????????
     * @param withUser      ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     */
    public List<Message> getMessagesByMessageStatus(Conversation conversation, List<Integer> messageStatus, long fromIndex, boolean before, int count, String withUser) {
        try {
            return mClient.getMessagesInStatusSync(conversation, convertIntegers(messageStatus), fromIndex, before, count, withUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ????????????????????????????????????
     *
     * @param conversation  ??????
     * @param messageStatus ??????????????????
     * @param fromIndex     ????????????id(messageId)
     * @param before        true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count         ??????????????????
     * @param withUser      ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback      ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessagesByMessageStatus(Conversation conversation, List<Integer> messageStatus, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getMessagesInStatusAsync(conversation, convertIntegers(messageStatus), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param conversationTypes ????????????
     * @param lines             ????????????
     * @param contentTypes      ????????????
     * @param fromIndex         ????????????id(messageId)
     * @param before            true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count             ??????????????????
     * @param withUser          ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback          ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0 ||
                contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.getMessagesExAsync(intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));

                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param conversationTypes ????????????
     * @param lines             ????????????
     * @param messageStatus     ????????????
     * @param fromIndex         ????????????id(messageId)
     * @param before            true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count             ??????????????????
     * @param withUser          ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback          ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessagesEx2(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<MessageStatus> messageStatus, long fromIndex, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }

        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        int[] status = new int[messageStatus.size()];
        for (int i = 0; i < messageStatus.size(); i++) {
            status[i] = messageStatus.get(i).ordinal();
        }

        try {
            mClient.getMessagesEx2Async(intypes, convertIntegers(lines), status, fromIndex, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param conversation ??????
     * @param contentTypes ??????????????????
     * @param timestamp    ?????????
     * @param before       true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count        ??????????????????
     * @param withUser     ?????????????????????{@link cn.wildfirechat.model.Conversation.ConversationType#Channel}?????????, channel?????????????????????????????????????????????
     * @param callback     ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getMessagesByTimestamp(Conversation conversation, List<Integer> contentTypes, long timestamp, boolean before, int count, String withUser, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getMessagesInTypesAndTimestampAsync(conversation, convertIntegers(contentTypes), timestamp, before, count, withUser, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????
     *
     * @param userId       userId
     * @param conversation ??????
     * @param fromIndex    ????????????id(messageId)
     * @param before       true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count        ??????????????????
     * @param callback     ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getUserMessages(String userId, Conversation conversation, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getUserMessages(userId, conversation, fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param userId            userId
     * @param conversationTypes ????????????
     * @param lines             ????????????
     * @param contentTypes      ????????????
     * @param fromIndex         ????????????id(messageId)
     * @param before            true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count             ??????????????????
     * @param callback          ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void getUserMessagesEx(String userId, List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            Log.e(TAG, "Remote service not available");
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0 ||
                contentTypes == null || contentTypes.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines or contentType");
            callback.onFail(ErrorCode.INVALID_PARAMETER);
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.getUserMessagesEx(userId, intypes, convertIntegers(lines), convertIntegers(contentTypes), fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));

                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????????????????
     *
     * @param conversation     ??????
     * @param beforeMessageUid ?????????????????????uid
     * @param count            ?????????????????????
     * @param callback
     * @discussion ???????????????????????????????????????????????????count????????????count??????0??????????????????????????????????????????????????????????????????????????????0????????????????????????????????????
     */
    public void getRemoteMessages(Conversation conversation, List<Integer> contentTypes, long beforeMessageUid, int count, GetRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int[] intypes = null;
            if (contentTypes != null && !contentTypes.isEmpty()) {
                intypes = new int[contentTypes.size()];
                for (int i = 0; i < contentTypes.size(); i++) {
                    intypes[i] = contentTypes.get(i);
                }
            }
            List<Message> outMsgs = new ArrayList<>();
            mClient.getRemoteMessages(conversation, intypes, beforeMessageUid, count, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        outMsgs.addAll(messages);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(outMsgs));
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param conversation     ??????
     * @param beforeMessageUid ?????????????????????uid
     * @param count            ?????????????????????
     * @param callback
     * @discussion ???????????????????????????????????????????????????count????????????count??????0??????????????????????????????????????????????????????????????????????????????0????????????????????????????????????
     */
    public void getRemoteMessagesAndSave(Conversation conversation, List<Integer> contentTypes, long beforeMessageUid, int count, GetRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int[] intypes = null;
            if (contentTypes != null && !contentTypes.isEmpty()) {
                intypes = new int[contentTypes.size()];
                for (int i = 0; i < contentTypes.size(); i++) {
                    intypes[i] = contentTypes.get(i);
                }
            }
            List<Message> outMsgs = new ArrayList<>();
            mClient.getRemoteMessagesAndSave(conversation, intypes, beforeMessageUid, count, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        outMsgs.addAll(messages);
                        if (!hasMore) {
                            mainHandler.post(() -> callback.onSuccess(outMsgs));
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????
     *
     * @param messageUid ??????uid
     * @param callback
     */
    public void getRemoteMessage(long messageUid, GetOneRemoteMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            List<Message> outMsgs = new ArrayList<>();
            mClient.getRemoteMessage(messageUid, new IGetRemoteMessagesCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    if (callback != null) {
                        outMsgs.addAll(messages);
                        if (!hasMore) {
                            mainHandler.post(() -> {
                                callback.onSuccess(messages.get(0));
                            });
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????
     *
     * @param conversation    ??????????????????????????????????????????????????????????????????????????????
     * @param fromUser        ????????????????????????????????????????????????????????????????????????
     * @param beforeMessageId ?????????????????????id
     * @param order           ??????????????????
     * @param count           ?????????????????????
     * @param callback
     */
    public void getConversationFileRecords(Conversation conversation, String fromUser, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.getConversationFileRecords(conversation, fromUser, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getMyFileRecords(long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.getMyFileRecords(beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void deleteFileRecord(long messageUid, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteFileRecord(messageUid, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    public void searchMyFileRecords(String keyword, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.searchMyFileRecords(keyword, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????
     *
     * @param keyword         ?????????
     * @param conversation    ??????????????????????????????????????????????????????????????????????????????
     * @param fromUser        ????????????????????????????????????????????????????????????????????????
     * @param beforeMessageId ?????????????????????id
     * @param count           ?????????????????????
     * @param callback
     */
    public void searchFileRecords(String keyword, Conversation conversation, String fromUser, long beforeMessageId, FileRecordOrder order, int count, GetFileRecordCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int intOrder = 0;
            if (order != null) {
                intOrder = order.value;
            }
            mClient.searchFileRecords(keyword, conversation, fromUser, beforeMessageId, intOrder, count, new IGetFileRecordCallback.Stub() {
                @Override
                public void onSuccess(List<FileRecord> messages) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess(messages);
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????id???????????????
     *
     * @param messageId ??????id
     *                  <p>
     *                  ??????uid?????????uid?????????id?????????????????????????????????uid??????uid????????????????????????????????????id??????????????????
     *                  ???????????????????????????{@link cn.wildfirechat.message.core.PersistFlag}??????????????????????????????
     *                  {@link cn.wildfirechat.message.core.PersistFlag#Persist_And_Count}
     *                  ???{@link cn.wildfirechat.message.core.PersistFlag#Persist}?????????????????????id
     * @return
     */
    public Message getMessage(long messageId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessage(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ????????????uid???????????????
     *
     * @param messageUid ??????uid???
     *                   <p>
     *                   ??????uid?????????id?????????????????????????????????uid??????uid????????????????????????????????????id??????????????????
     *                   <p>
     *                   ???????????????????????????{@link cn.wildfirechat.message.core.PersistFlag}??????????????????????????????
     *                   {@link cn.wildfirechat.message.core.PersistFlag#Persist_And_Count}
     *                   ???{@link cn.wildfirechat.message.core.PersistFlag#Persist}?????????????????????id
     * @return
     */
    public Message getMessageByUid(long messageUid) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessageByUid(messageUid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ???????????????????????????
     *
     * @param conversation
     * @return
     */
    public UnreadCount getUnreadCount(Conversation conversation) {
        if (!checkRemoteService()) {
            return new UnreadCount();
        }

        try {
            return mClient.getUnreadCount(conversation.type.ordinal(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new UnreadCount();
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param conversationTypes
     * @param lines
     * @return
     */
    public UnreadCount getUnreadCountEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return new UnreadCount();
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.getUnreadCountEx(intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new UnreadCount();
    }


    /**
     * ?????????????????????????????????
     *
     * @param conversation
     */
    public void clearUnreadStatus(Conversation conversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            if (mClient.clearUnreadStatus(conversation.type.getValue(), conversation.target, conversation.line)) {
                ConversationInfo conversationInfo = getConversation(conversation);
                conversationInfo.unreadCount = new UnreadCount();
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    listener.onConversationUnreadStatusClear(conversationInfo);
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void clearUnreadStatusEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return;
        }
        int[] inTypes = new int[conversationTypes.size()];
        int[] inLines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            inTypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inLines[j] = lines.get(j);
        }

        try {
            boolean result = mClient.clearUnreadStatusEx(inTypes, inLines);
            if (result) {
                List<ConversationInfo> conversationInfos = mClient.getConversationList(inTypes, inLines);
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    for (ConversationInfo info : conversationInfos) {
                        listener.onConversationUnreadStatusClear(info);
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean markAsUnRead(Conversation conversation, boolean syncToOtherClient) {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            boolean result = mClient.markAsUnRead(conversation.type.getValue(), conversation.target, conversation.line, syncToOtherClient);
            if (result) {
                ConversationInfo conversationInfo = getConversation(conversation);
                for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                    listener.onConversationUnreadStatusClear(conversationInfo);
                }
            }
            return result;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearMessageUnreadStatus(long messageId) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            Message msg = getMessage(messageId);
            if (msg != null) {
                if (mClient.clearMessageUnreadStatus(messageId)) {
                    ConversationInfo conversationInfo = getConversation(msg.conversation);
                    for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                        listener.onConversationUnreadStatusClear(conversationInfo);
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????audio??????media??????????????????????????????(??????????????????????????????????????????????????????????????????????????????????????????)
     *
     * @param messageId
     */
    public void setMediaMessagePlayed(long messageId) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setMediaMessagePlayed(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param messageId ??????ID
     * @param extra     ????????????
     * @return true???????????????false????????????
     */
    public boolean setMessageLocalExtra(long messageId, String extra) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.setMessageLocalExtra(messageId, extra);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ?????????????????????????????????
     */
    public void clearAllUnreadStatus() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearAllUnreadStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     *
     * @param conversation
     */
    public void clearMessages(Conversation conversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearMessages(conversation.type.getValue(), conversation.target, conversation.line);

            for (OnClearMessageListener listener : clearMessageListeners) {
                listener.onClearMessage(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     *
     * @param conversation
     * @param beforeTime
     */
    public void clearMessages(Conversation conversation, long beforeTime) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            int convType = 0;
            String target = "";
            int line = 0;
            if (conversation != null) {
                convType = conversation.type.getValue();
                target = conversation.target;
                line = conversation.line;
            }
            mClient.clearMessagesEx(convType, target, line, beforeTime);

            for (OnClearMessageListener listener : clearMessageListeners) {
                listener.onClearMessage(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * ??????????????????
     *
     * @param removeConversation ??????????????????????????????.
     */
    public void clearAllMessages(boolean removeConversation) {
        if (!checkRemoteService()) {
            return;
        }

        try {

            mClient.clearAllMessages(removeConversation);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param conversation
     * @param clearMsg     ??????????????????????????????????????????
     */
    public void removeConversation(Conversation conversation, boolean clearMsg) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.removeConversation(conversation.type.ordinal(), conversation.target, conversation.line, clearMsg);
            for (OnRemoveConversationListener listener : removeConversationListeners) {
                listener.onConversationRemove(conversation);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void clearRemoteConversationMessage(Conversation conversation, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.clearRemoteConversationMessage(conversation, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_DIED));
        }
    }

    public void setConversationTop(Conversation conversation, int top) {
        setConversationTop(conversation, top, null);
    }

    /**
     * ????????????
     *
     * @param conversation
     * @param top          true????????????false???????????????
     */
    public void setConversationTop(Conversation conversation, int top, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setConversationTop(conversation.type.ordinal(), conversation.target, conversation.line, top, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    ConversationInfo conversationInfo = getConversation(conversation);
                    mainHandler.post(() -> {
                        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                            listener.onConversationTopUpdate(conversationInfo, top);
                        }

                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * ??????????????????
     *
     * @param conversation
     * @param draft
     */
    public void setConversationDraft(Conversation conversation, @Nullable String draft) {
        if (conversation == null) {
            return;
        }

        if (!checkRemoteService()) {
            return;
        }

        try {
            ConversationInfo conversationInfo = getConversation(conversation);
            if (conversationInfo == null || TextUtils.equals(draft, conversationInfo.draft)) {
                return;
            }
            mClient.setConversationDraft(conversation.type.ordinal(), conversation.target, conversation.line, draft);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        ConversationInfo conversationInfo = getConversation(conversation);
        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
            listener.onConversationDraftUpdate(conversationInfo, draft);
        }
    }

    /**
     * ???????????????????????????
     *
     * @param conversation ????????????????????????????????????
     * @return key-value, ????????????userId????????????????????????????????????????????????????????????????????????server???????????????????????????serverTime???????????????????????????
     */
    public Map<String, Long> getConversationRead(Conversation conversation) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getConversationRead(conversation.type.getValue(), conversation.target, conversation.line);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ???????????????????????????
     *
     * @param conversation ????????????????????????????????????
     * @return
     */
    public Map<String, Long> getMessageDelivery(Conversation conversation) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMessageDelivery(conversation.type.getValue(), conversation.target);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?????????????????????, ??????????????????????????????????????????????????????
     *
     * @param conversation
     * @param timestamp
     */
    public void setConversationTimestamp(Conversation conversation, long timestamp) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationTimestamp(conversation.type.ordinal(), conversation.target, conversation.line, timestamp);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param keyword
     * @param searchUserType
     * @param page
     * @param callback
     */
    public void searchUser(String keyword, SearchUserType searchUserType, int page, final SearchUserCallback callback) {
//        if (userSource != null) {
//            userSource.searchUser(keyword, callback);
//            return;
//        }
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.searchUser(keyword, searchUserType.ordinal(), page, new cn.wildfirechat.client.ISearchUserCallback.Stub() {
                @Override
                public void onSuccess(final List<UserInfo> userInfos) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(userInfos);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????????????????
     *
     * @param userId
     * @return
     */
    public boolean isMyFriend(String userId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isMyFriend(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ????????????id??????
     *
     * @param refresh ????????????????????????????????????????????????????????????????????????????????????????????????????????????{@link OnFriendUpdateListener}????????????
     * @return
     */
    public List<String> getMyFriendList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getMyFriendList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????
     *
     * @param refresh ????????????????????????????????????????????????????????????????????????????????????????????????????????????{@link OnFriendUpdateListener}????????????
     * @return
     */
    public List<Friend> getFriendList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ??????????????????
    // 1. ????????? 2. ???????????? 3. ??????displayName 4. <uid>
    public String getGroupMemberDisplayName(String groupId, String memberId) {
        UserInfo userInfo = getUserInfo(memberId, groupId, false);
        if (userInfo == null) {
            return "<" + memberId + ">";
        }
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            return userInfo.groupAlias;
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        return "<" + memberId + ">";
    }

    public String getGroupMemberDisplayName(UserInfo userInfo) {
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            return userInfo.groupAlias;
        } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.nickName)) {
            return userInfo.nickName;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        if (!TextUtils.isEmpty(userInfo.memberName)) {
            return "<" + userInfo.memberName + ">";
        } else {
            return "<" + userInfo.uid + ">";
        }
    }

    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) {
            return "";
        }
        if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            return userInfo.friendAlias;
        } else if (!TextUtils.isEmpty(userInfo.nickName)) {
            return userInfo.nickName;
        } else if (!TextUtils.isEmpty(userInfo.displayName)) {
            return userInfo.displayName;
        }
        if (!TextUtils.isEmpty(userInfo.memberName)) {
            return "<" + userInfo.memberName + ">";
        } else {
            return "<" + userInfo.uid + ">";
        }
    }

    public String getUserDisplayName(String userId) {
        UserInfo userInfo = getUserInfo(userId, false);
        return getUserDisplayName(userInfo);
    }

    public String getUserPortrait(UserInfo userInfo) {
        if (!TextUtils.isEmpty(userInfo.avatar) && (userInfo.avatar.contains("http") || userInfo.avatar.contains("{{domain}}"))) {
            return userInfo.avatar;
        } else if (!TextUtils.isEmpty(userInfo.portrait) && (userInfo.portrait.contains("http") || userInfo.portrait.contains("{{domain}}"))) {
            return userInfo.portrait;
        } else {
            return "";
        }
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @return
     */
    public String getFriendAlias(String userId) {
        if (!checkRemoteService()) {
            return null;
        }
        String alias;
        try {
            alias = mClient.getFriendAlias(userId);
            return alias;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @param alias
     * @param callback
     */
    public void setFriendAlias(String userId, String alias, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setFriendAlias(userId, alias, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(callback::onSuccess);
                    }

                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onFail(errorCode);
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     *
     * @param refresh
     * @return
     */
    public List<UserInfo> getMyFriendListInfo(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            List<String> userIds = mClient.getMyFriendList(refresh);
            if (userIds != null && !userIds.isEmpty()) {
                List<UserInfo> userInfos = new ArrayList<>();
                int step = 400;
                int startIndex, endIndex;
                for (int i = 0; i <= userIds.size() / step; i++) {
                    startIndex = i * step;
                    endIndex = (i + 1) * step;
                    endIndex = Math.min(endIndex, userIds.size());
                    List<UserInfo> us = mClient.getUserInfos(userIds.subList(startIndex, endIndex), null);
                    userInfos.addAll(us);
                }
                if (userInfos.size() > 0) {
                    for (UserInfo info : userInfos) {
                        if (info != null) {
                            userInfoCache.put(info.uid, info);
                        }
                    }
                }
                return userInfos;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * ??????????????????????????????
     */
    public void loadFriendRequestFromRemote() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.loadFriendRequestFromRemote();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????
     *
     * @param incoming true????????????????????????????????????false?????????????????????
     * @return
     */
    public List<FriendRequest> getFriendRequest(boolean incoming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getFriendRequest(incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????
     *
     * @param userId   ????????????Id
     * @param incoming true????????????????????????????????????false?????????????????????
     * @return
     */
    public FriendRequest getFriendRequest(String userId, boolean incoming) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getOneFriendRequest(userId, incoming);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????????????????
     */
    public void clearUnreadFriendRequestStatus() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.clearUnreadFriendRequestStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    public int getUnreadFriendRequestStatus() {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getUnreadFriendRequestStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * ????????????
     *
     * @param userId
     * @param callback
     */
    public void removeFriend(String userId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.removeFriend(userId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_EXCEPTION);
        }
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @param reason
     * @param callback
     */
    public void sendFriendRequest(String userId, String reason, String extra, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.sendFriendRequest(userId, reason, extra, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @param accept
     * @param extra    ???????????????????????????extra?????????????????????extra???????????????json??????????????????????????????????????????
     * @param callback
     */
    public void handleFriendRequest(String userId, boolean accept, String extra, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.handleFriendRequest(userId, accept, extra, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param userId
     * @return
     */
    public boolean isBlackListed(String userId) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isBlackListed(userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * ?????????????????????
     *
     * @param refresh
     * @return
     */
    public List<String> getBlackList(boolean refresh) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getBlackList(refresh);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @param userId
     * @param isBlacked
     * @param callback
     */
    public void setBlackList(String userId, boolean isBlacked, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setBlackList(userId, isBlacked, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param userId
     * @param callback
     */
    public void deleteFriend(String userId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteFriend(userId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }


    /**
     * ???????????????
     *
     * @param groupId
     * @param refresh
     * @return
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true????????????????????????????????????????????????true???
     */
    public @Nullable
    GroupInfo getGroupInfo(String groupId, boolean refresh) {
        if (!checkRemoteService()) {
            return new NullGroupInfo(groupId);
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.d(TAG, "get group info error, group id is empty");
            return null;
        }

        try {
            GroupInfo groupInfo = mClient.getGroupInfo(groupId, refresh);
            if (groupInfo == null) {
                groupInfo = new NullGroupInfo(groupId);
            }
            return groupInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ???????????????
     *
     * @param groupId
     * @param refresh
     * @param callback
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true????????????????????????????????????????????????true???
     */
    public void getGroupInfo(String groupId, boolean refresh, GetGroupInfoCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.getGroupInfoEx(groupId, refresh, new IGetGroupCallback.Stub() {
                @Override
                public void onSuccess(GroupInfo userInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(userInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });

        } catch (RemoteException e) {
            e.printStackTrace();

        }
    }

    /**
     * ???????????????
     *
     * @param chatRoomId
     * @param callback
     */
    public void joinChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(chatRoomId)) {
            Log.e(TAG, "Error, chatroomid is empty");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.joinChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????
     *
     * @param chatRoomId
     * @param callback
     */
    public void quitChatRoom(String chatRoomId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.quitChatRoom(chatRoomId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ?????????????????????
     *
     * @param chatRoomId
     * @param updateDt
     * @param callback
     */
    public void getChatRoomInfo(String chatRoomId, long updateDt, GetChatRoomInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.getChatRoomInfo(chatRoomId, updateDt, new cn.wildfirechat.client.IGetChatRoomInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomInfo chatRoomInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(chatRoomInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    public void putChatRoomCache(List<CustomChatRoomInfo> chatRoomList) {
        if (chatroomCache != null && chatRoomList != null) {
            CustomChatRoomInfo info;
            for (int index = 0; index < chatRoomList.size(); index++) {
                info = chatRoomList.get(index);
                chatroomCache.put(info.cid, info);
            }
        }
    }

    public CustomChatRoomInfo getChatRoomCache(String chatRoomId) {
        if (chatroomCache != null) {
            return chatroomCache.get(chatRoomId);
        } else {
            return null;
        }
    }

    /**
     * ???????????????????????????
     *
     * @param chatRoomId
     * @param maxCount   ?????????????????????????????????
     * @param callback
     */
    public void getChatRoomMembersInfo(String chatRoomId, int maxCount, GetChatRoomMembersInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.getChatRoomMembersInfo(chatRoomId, maxCount, new cn.wildfirechat.client.IGetChatRoomMembersInfoCallback.Stub() {
                @Override
                public void onSuccess(ChatRoomMembersInfo chatRoomMembersInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(chatRoomMembersInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param userId
     * @param refresh
     * @return
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true??????????????????????????????????????????????????????????????????????????????????????????true???
     */
    public UserInfo getUserInfo(String userId, boolean refresh) {
        return getUserInfo(userId, null, refresh);
    }

    /**
     * ?????? ??????????????????
     *
     * @param userId
     * @param refresh
     * @return
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true??????????????????????????????????????????????????????????????????????????????????????????true???
     */
    public void getUserInfoAsync(String userId, String groupId, boolean refresh, UserInfoCallback<UserInfo> callback) {
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            callback.onUiFailure(-1, "Error, user id is null");
            return;
        }
        UserInfo userInfo = null;
        if (!refresh) {
            if (TextUtils.isEmpty(groupId)) {
                userInfo = userInfoCache.get(userId);
            }
            if (userInfo != null) {
                callback.onUiSuccess(userInfo);
            } else {
                callback.onUiSuccess(new NullUserInfo(userId));
            }
            return;
        }
        if (userSource != null) {
            userSource.getUser(userId, new UserInfoCallback<UserInfo>() {
                @Override
                public void onUiSuccess(UserInfo userInfo) {
                    if (callback != null && userInfo != null) {
                        userInfoCache.put(userId, userInfo);
                        callback.onUiSuccess(userInfo);
                    }
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    if (callback != null) {
                        callback.onFailure(-1, msg);
                    }
                }
            });
            return;
        } else {
            callback.onUiFailure(-1, "Error, user id is null");
        }
    }

    /**
     * ????????????????????????????????????????????????{@link UserInfo}???{@link NullUserInfo}
     *
     * @param userId
     * @param groupId
     * @param refresh
     * @return
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true??????????????????????????????????????????????????????????????????????????????????????????true???
     */
    public UserInfo getUserInfoByIM(String userId, String groupId, boolean refresh) {
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            return null;
        }
        UserInfo userInfo = null;
        if (!refresh) {
            if (TextUtils.isEmpty(groupId)) {
                userInfo = userInfoCache.get(userId);
            }
            if (userInfo != null) {
                return userInfo;
            }
        }

        if (!checkRemoteService()) {
            return new NullUserInfo(userId);
        }

        try {
            userInfo = mClient.getUserInfo(userId, groupId, refresh);
            if (userInfo == null) {
                userInfo = new NullUserInfo(userId);
            } else {
                if (TextUtils.isEmpty(groupId)) {
                    userInfoCache.put(userId, userInfo);
                }
            }
            return userInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return new NullUserInfo(userId);
        }
    }

    /**
     * ????????????????????????????????????????????????{@link UserInfo}???{@link NullUserInfo}
     *
     * @param userId
     * @param groupId
     * @param refresh
     * @return
     * @discussion refresh ???true????????????????????????????????????????????????????????????????????????true??????????????????????????????????????????????????????????????????????????????????????????true???
     */
    public UserInfo getUserInfo(String userId, String groupId, boolean refresh) {
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            return null;
        }
        UserInfo userInfo = null;
        if (!refresh) {
            if (TextUtils.isEmpty(groupId)) {
                userInfo = userInfoCache.get(userId);
            }
            if (userInfo != null) {
                return userInfo;
            }
        }
//        if (userSource != null) {
//            userInfo = userSource.getUser(userId, null);
//            if (userInfo == null) {
//                userInfo = new NullUserInfo(userId);
//            }
//            return userInfo;
//        }

        if (!checkRemoteService()) {
            return new NullUserInfo(userId);
        }

        try {
            userInfo = mClient.getUserInfo(userId, groupId, refresh);
            if (userInfo == null) {
                userInfo = new NullUserInfo(userId);
            } else {
                if (TextUtils.isEmpty(groupId)) {
                    userInfoCache.put(userId, userInfo);
                }
            }
            return userInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
            return new NullUserInfo(userId);
        }
    }

    /**
     * ?????????list????????????????????????null
     *
     * @param userIds
     * @param groupId
     * @return
     */
    public List<UserInfo> getUserInfos(List<String> userIds, String groupId) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
//        if (userSource != null) {
//            List<UserInfo> userInfos = new ArrayList<>();
//            for (String userId : userIds) {
//                userInfos.add(userSource.getUser(userId, null));
//            }
//            return userInfos;
//        }

        if (!checkRemoteService()) {
            return null;
        }

        try {
            List<UserInfo> userInfos = new ArrayList<>();
            int step = 400;
            int startIndex, endIndex;
            for (int i = 0; i <= userIds.size() / step; i++) {
                startIndex = i * step;
                endIndex = (i + 1) * step;
                endIndex = Math.min(endIndex, userIds.size());
                List<UserInfo> us = mClient.getUserInfos(userIds.subList(startIndex, endIndex), groupId);
                userInfos.addAll(us);
            }
            if (userInfos.size() > 0) {
                for (UserInfo info : userInfos) {
                    if (info != null) {
                        if (TextUtils.isEmpty(groupId)) {
                            userInfoCache.put(info.uid, info);
                        }
                    }
                }
            }

            Collections.sort(userInfos, (o1, o2) -> {
                int index_1 = userIds.indexOf(o1.uid);
                int index_2 = userIds.indexOf(o2.uid);
                return index_1 - index_2;
            });

            return userInfos;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void getUserInfo(String userId, boolean refresh, GetUserInfoCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }
        try {
            mClient.getUserInfoEx(userId, refresh, new IGetUserCallback.Stub() {
                @Override
                public void onSuccess(UserInfo userInfo) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(userInfo));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     *
     * @param mediaPath
     * @param mediaType ?????????????????????????????????{@link cn.wildfirechat.message.MessageContentMediaType}
     * @param callback
     */
    public void uploadMediaFile(String mediaPath, int mediaType, final UploadMediaCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.uploadMediaFile(mediaPath, mediaType, new IUploadMediaCallback.Stub() {
                @Override
                public void onSuccess(final String remoteUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(remoteUrl);
                            }
                        });
                    }
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {
                    callback.onProgress(uploaded, total);
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }


    /**
     * @param data      ????????????1M??????????????????????????????900K
     * @param mediaType ??????????????????????????????{@link cn.wildfirechat.message.MessageContentMediaType}
     * @param callback
     */
    public void uploadMedia(String fileName, byte[] data, int mediaType, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (data.length > 900 * 1024) {
            if (callback != null) {
                callback.onFail(ErrorCode.FILE_TOO_LARGE);
            }
            return;
        }

        try {
            mClient.uploadMedia(fileName, data, mediaType, new IUploadMediaCallback.Stub() {
                @Override
                public void onSuccess(final String remoteUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(remoteUrl);
                            }
                        });
                    }
                }

                @Override
                public void onProgress(final long uploaded, final long total) throws RemoteException {

                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????
     *
     * @param values
     * @param callback
     */
    public void modifyMyInfo(List<ModifyMyInfoEntry> values, final GeneralCallback callback) {
//        userInfoCache.remove(userId);
        if (userSource != null) {
            userSource.modifyMyInfo(values, new UserInfoCallback<UserInfo>() {
                @Override
                public void onUiSuccess(UserInfo userInfo) {
                    if (userInfo != null) {
                        userInfoCache.put(userInfo.uid, userInfo);
                    }
                    callback.onSuccess();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onFail(code);
                }
            });
            return;
        }
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.modifyMyInfo(values, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }

                    UserInfo userInfo = getUserInfo(userId, false);
                    onUserInfoUpdate(Collections.singletonList(userInfo));
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }

    }

    /**
     * ??????????????????
     *
     * @param message
     * @return
     */
    public boolean deleteMessage(Message message) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.deleteMessage(message.messageId);
            for (OnDeleteMessageListener listener : deleteMessageListeners) {
                listener.onDeleteMessage(message);
            }
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ????????????????????????
     *
     * @param messageUids ?????????Uid??????
     * @return
     */
    public boolean batchDeleteMessages(List<Long> messageUids) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            long[] uids = new long[messageUids.size()];
            for (int i = 0; i < messageUids.size(); i++) {
                uids[i] = messageUids.get(i);
            }
            mClient.batchDeleteMessages(uids);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param userId    ????????????
     * @param startTime ??????????????????0?????????????????????
     * @param endTime   ??????????????????0????????????????????????
     * @return
     */
    public boolean clearUserMessage(String userId, long startTime, long endTime) {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            mClient.clearUserMessage(userId, startTime, endTime);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param messageUid ?????????UID
     * @param callback   ??????????????????
     */
    public void deleteRemoteMessage(long messageUid, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.deleteRemoteMessage(messageUid, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        onDeleteMessage(messageUid);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????Server API????????????????????????
     *
     * @param messageUid     ?????????UID
     * @param messageContent ????????????
     * @param distribute     ??????????????????????????????
     * @param updateLocal    ??????????????????????????????
     * @param callback       ??????????????????
     */
    public void updateRemoteMessageContent(long messageUid, MessageContent messageContent, boolean distribute, boolean updateLocal, GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.updateRemoteMessageContent(messageUid, messageContent.encode(), distribute, updateLocal, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        onDeleteMessage(messageUid);
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param keyword
     * @param conversationTypes
     * @param lines
     * @return
     */
    public List<ConversationSearchResult> searchConversation(String keyword, List<Conversation.ConversationType> conversationTypes, List<Integer> lines) {
        if (!checkRemoteService()) {
            return null;
        }

        int[] intypes = new int[conversationTypes.size()];
        int[] inlines = new int[lines.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            return mClient.searchConversation(keyword, intypes, inlines);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param conversation ??????????????????????????????????????????
     * @param keyword
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMessage(Conversation conversation, String keyword, boolean desc, int limit, int offset) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessage(conversation, keyword, desc, limit, offset);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param conversation ??????????????????????????????????????????
     * @param keyword
     * @param contentTypes
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMessageByTypes(Conversation conversation, String keyword, List<Integer> contentTypes, boolean desc, int limit, int offset) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessageByTypes(conversation, keyword, convertIntegers(contentTypes), desc, limit, offset);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param conversation ??????????????????????????????????????????
     * @param keyword
     * @param contentTypes
     * @param startTime
     * @param endTime
     * @param desc
     * @param limit
     * @param offset
     * @return
     */
    public List<Message> searchMessageByTypesAndTimes(Conversation conversation, String keyword, List<Integer> contentTypes, long startTime, long endTime, boolean desc, int limit, int offset) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchMessageByTypesAndTimes(conversation, keyword, convertIntegers(contentTypes), startTime, endTime, desc, limit, offset);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param conversationTypes ????????????
     * @param lines             ????????????
     * @param contentTypes      ????????????
     * @param keyword           ???????????????
     * @param fromIndex         ????????????id(messageId)
     * @param before            true, ??????fromIndex???????????????????????????????????????false?????????fromIndex???????????????????????????????????????????????????fromIndex???????????????
     * @param count             ??????????????????
     * @param callback          ???????????????????????????????????????????????????????????????????????????????????????
     */
    public void searchMessagesEx(List<Conversation.ConversationType> conversationTypes, List<Integer> lines, List<Integer> contentTypes, String keyword, long fromIndex, boolean before, int count, GetMessageCallback callback) {
        if (!checkRemoteService()) {
            return;
        }
        if (conversationTypes == null || conversationTypes.size() == 0 ||
                lines == null || lines.size() == 0) {
            Log.e(TAG, "Invalid conversation type or lines");
            return;
        }

        int[] intypes = new int[conversationTypes.size()];
        for (int i = 0; i < conversationTypes.size(); i++) {
            intypes[i] = conversationTypes.get(i).ordinal();
        }

        try {
            mClient.searchMessagesEx(intypes, convertIntegers(lines), convertIntegers(contentTypes), keyword, fromIndex, before, count, new IGetMessageCallback.Stub() {
                @Override
                public void onSuccess(List<Message> messages, boolean hasMore) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(messages, hasMore));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     *
     * @param keyword
     * @return
     */
    public List<GroupSearchResult> searchGroups(String keyword) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchGroups(keyword);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param keyword
     * @return
     */
    public List<UserInfo> searchFriends(String keyword) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.searchFriends(keyword);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEncodedClientId() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getEncodedClientId();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void requireLock(String lockId, long duration, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.requireLock(lockId, duration, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    public void releaseLock(String lockId, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.releaseLock(lockId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param groupId
     * @param groupName
     * @param groupPortrait ??????????????????????????????????????????????????????
     * @param groupType
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void createGroup(String groupId, String groupName, String groupPortrait, GroupInfo.GroupType groupType, String groupExtra, List<String> memberIds, String memberExtra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.createGroup(groupId, groupName, groupPortrait, groupType.value(), groupExtra, memberIds, memberExtra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(final String result) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????
     *
     * @param groupId
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void addGroupMembers(String groupId, List<String> memberIds, String extra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.addGroupMembers(groupId, memberIds, extra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    private MessagePayload content2Payload(MessageContent content) {
        if (content == null) {
            return null;
        }
        MessagePayload payload = content.encode();
        payload.type = content.getClass().getAnnotation(ContentTag.class).type();
        return payload;
    }

    /**
     * ???????????????
     *
     * @param groupId
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void removeGroupMembers(String groupId, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.removeGroupMembers(groupId, memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param groupId
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void quitGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.quitGroup(groupId, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ????????????
     *
     * @param groupId
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void dismissGroup(String groupId, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.dismissGroup(groupId, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ?????? ?????????
     *
     * @param groupId
     * @param modifyType
     * @param newValue
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupInfo(groupId, modifyType.ordinal(), newValue, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    GroupInfo groupInfo = mClient.getGroupInfo(groupId, false);
                    onGroupInfoUpdated(Collections.singletonList(groupInfo));
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param groupId
     * @param alias
     * @param lines
     * @param notifyMsg
     * @param callback  ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????{@link OnGroupMembersUpdateListener}????????????
     */
    public void modifyGroupAlias(String groupId, String alias, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupAlias(groupId, alias, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param groupId
     * @param memberId
     * @param alias
     * @param lines
     * @param notifyMsg
     * @param callback  ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????{@link OnGroupMembersUpdateListener}????????????
     */
    public void modifyGroupMemberAlias(String groupId, String memberId, String alias, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupMemberAlias(groupId, memberId, alias, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param groupId
     * @param memberId
     * @param extra
     * @param lines
     * @param notifyMsg
     * @param callback  ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????{@link OnGroupMembersUpdateListener}????????????
     */
    public void modifyGroupMemberExtra(String groupId, String memberId, String extra, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }
        try {
            mClient.modifyGroupMemberExtra(groupId, memberId, extra, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                groupMemberCache.remove(groupMemberCacheKey(groupId, userId));
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ?????????????????????
     *
     * @param groupId
     * @param forceUpdate
     * @return
     * @discussion forceUpdate ???true????????????????????????????????????????????????????????????????????????true???????????????????????????????????????????????????true???
     */
    public List<GroupMember> getGroupMembers(String groupId, boolean forceUpdate) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            return null;
        }
        try {
            return mClient.getGroupMembers(groupId, forceUpdate);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<GroupMember> getGroupMembersByType(String groupId, GroupMember.GroupMemberType type) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return null;
        }

        try {
            return mClient.getGroupMembersByType(groupId, type.value());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<GroupMember> getGroupMembersByCount(String groupId, int count) {
        if (!checkRemoteService()) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "group id is null");
            return null;
        }

        try {
            return mClient.getGroupMembersByCount(groupId, count);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?????????????????????
     *
     * @param groupId
     * @param forceUpdate
     * @param callback
     * @discussion forceUpdate ???true????????????????????????????????????????????????????????????????????????true???????????????????????????????????????????????????true???
     */
    public void getGroupMembers(String groupId, boolean forceUpdate, GetGroupMembersCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        try {
            mClient.getGroupMemberEx(groupId, forceUpdate, new IGetGroupMemberCallback.Stub() {
                @Override
                public void onSuccess(List<GroupMember> groupMembers) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(groupMembers));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String groupMemberCacheKey(String groupId, String memberId) {
        return memberId + "@" + groupId;
    }

    /**
     * ?????????????????????
     *
     * @param groupId
     * @param memberId
     * @return
     */
    public GroupMember getGroupMember(String groupId, String memberId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(memberId)) {
            return null;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            return null;
        }

        String key = groupMemberCacheKey(groupId, memberId);
        GroupMember groupMember = groupMemberCache.get(key);
        if (groupMember != null) {
            return groupMember;
        }

        if (!checkRemoteService()) {
            return null;
        }

        try {
            groupMember = mClient.getGroupMember(groupId, memberId);
            groupMemberCache.put(key, groupMember);
            return groupMember;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????
     *
     * @param groupId
     * @param newOwner
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void transferGroup(String groupId, String newOwner, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.transferGroup(groupId, newOwner, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /**
     * ??????????????????
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void setGroupManager(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.setGroupManager(groupId, isSet, memberIds, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ???????????????
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void muteGroupMember(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.muteOrAllowGroupMember(groupId, isSet, memberIds, false, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param groupId
     * @param isSet
     * @param memberIds
     * @param lines
     * @param notifyMsg
     * @param callback
     */
    public void allowGroupMember(String groupId, boolean isSet, List<String> memberIds, List<Integer> lines, MessageContent notifyMsg, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(groupId)) {
            Log.e(TAG, "Error, group id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        int[] inlines = new int[lines.size()];
        for (int j = 0; j < lines.size(); j++) {
            inlines[j] = lines.get(j);
        }

        try {
            mClient.muteOrAllowGroupMember(groupId, isSet, memberIds, true, inlines, content2Payload(notifyMsg), new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    public String getGroupRemark(String groupId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getGroupRemark(groupId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setGroupRemark(String groupId, String remark, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.setGroupRemark(groupId, remark, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    GroupInfo groupInfo = mClient.getGroupInfo(groupId, false);
                    onGroupInfoUpdated(Collections.singletonList(groupInfo));
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null) {
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
            }
        }
    }

    public byte[] encodeData(byte[] data) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.encodeData(data);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decodeData(byte[] data) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.decodeData(data);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decodeData(int type, byte[] data, boolean gzip) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.decodeDataEx(type, data, gzip);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHost() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getHost();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getPort() {
        if (!checkRemoteService()) {
            return 80;
        }

        try {
            return mClient.getPort();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 80;
        }
    }

    public String getHostEx() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getHostEx();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????
     *
     * @param scope ????????????????????????????????????????????????{@link UserSettingScope}
     * @param key
     * @return
     */
    public String getUserSetting(int scope, String key) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getUserSetting(scope, key);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ??????????????????
     *
     * @param scope ???????????????{@link UserSettingScope}
     * @return
     */
    public Map<String, String> getUserSettings(int scope) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return (Map<String, String>) mClient.getUserSettings(scope);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param callback
     */
    public void getFavGroups(final GetGroupsCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        workHandler.post(() -> {
            Map<String, String> groupIdMap = getUserSettings(UserSettingScope.FavoriteGroup);
            List<GroupInfo> groups = new ArrayList<>();
            if (groupIdMap != null && !groupIdMap.isEmpty()) {
                for (Map.Entry<String, String> entry : groupIdMap.entrySet()) {
                    if (entry.getValue().equals("1")) {
                        GroupInfo info = getGroupInfo(entry.getKey(), false);
                        if (!(info instanceof NullGroupInfo)) {
                            groups.add(getGroupInfo(entry.getKey(), false));
                        }
                    }
                }
            }
            mainHandler.post(() -> callback.onSuccess(groups));
        });
    }

    public boolean isFavGroup(String groupId) {
        if (!checkRemoteService()) {
            return false;
        }

        String value = getUserSetting(UserSettingScope.FavoriteGroup, groupId);
        if (value == null || !value.equals("1")) {
            return false;
        }
        return true;
    }

    public void setFavGroup(String groupId, boolean isSet, GeneralCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        setUserSetting(UserSettingScope.FavoriteGroup, groupId, isSet ? "1" : "0", callback);
    }

    public void getFavUsers(final StringListCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        workHandler.post(() -> {
            Map<String, String> userIdMap = getUserSettings(UserSettingScope.FavoriteUser);
            List<String> userIds = new ArrayList<>();
            if (userIdMap != null && !userIdMap.isEmpty()) {
                for (Map.Entry<String, String> entry : userIdMap.entrySet()) {
                    if (entry.getValue().equals("1")) {
                        userIds.add(entry.getKey());
                    }
                }
            }
            mainHandler.post(() -> callback.onSuccess(userIds));
        });
    }

    public boolean isFavUser(String userId) {
        if (!checkRemoteService()) {
            return false;
        }
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            return false;
        }

        String value = getUserSetting(UserSettingScope.FavoriteUser, userId);
        if (value == null || !value.equals("1")) {
            return false;
        }
        return true;
    }

    public void setFavUser(String userId, boolean isSet, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, "Error, user id is null");
            if (callback != null)
                callback.onFail(-1);
            return;
        }

        setUserSetting(UserSettingScope.FavoriteUser, userId, isSet ? "1" : "0", callback);
    }

    /*
    ??????????????????????????????????????????????????? getFavGroups
     */
    @Deprecated
    public void getMyGroups(final GetGroupsCallback callback) {
        getFavGroups(callback);
    }

    /**
     * ?????????????????????????????????
     *
     * @return ?????????????????????????????????
     */
    public Pair<Integer, String> getMyCustomState() {
        try {
            String str = getUserSetting(UserSettingScope.CustomState, "");
            if (TextUtils.isEmpty(str) || !str.contains("-")) {
                return new Pair<>(0, null);
            }
            int index = str.indexOf("-");
            int state = Integer.parseInt(str.substring(0, index));
            String text = str.substring(index + 1);
            return new Pair<>(state, text);
        } catch (Exception e) {
            return new Pair<>(0, null);
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param state    ???????????????
     * @param text     ?????????????????????
     * @param callback ??????????????????
     */
    public void setMyCustomState(int state, String text, GeneralCallback callback) {
        String str = state + "-";
        if (!TextUtils.isEmpty(text)) {
            str += text;
        }
        setUserSetting(UserSettingScope.CustomState, "", str, callback);
    }

    /**
     * ????????????????????????
     *
     * @param scope
     * @param key
     * @param value
     * @param callback
     */
    public void setUserSetting(int scope, String key, String value, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setUserSetting(scope, key, value, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess();
                            }
                        });
                    }
                    onSettingUpdated();
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????
     *
     * @param conversation
     * @param silent
     */
    public void setConversationSilent(Conversation conversation, boolean silent) {
        setConversationSilent(conversation, silent, null);
    }

    /**
     * ???????????????
     *
     * @param conversation
     * @param silent
     * @param callback
     */
    public void setConversationSilent(Conversation conversation, boolean silent, GeneralCallback callback) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setConversationSilent(conversation.type.ordinal(), conversation.target, conversation.line, silent, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> {
                        ConversationInfo conversationInfo = getConversation(conversation);
                        for (OnConversationInfoUpdateListener listener : conversationInfoUpdateListeners) {
                            listener.onConversationSilentUpdate(conversationInfo, silent);
                        }
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    });
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getAuthCode(String appId, int appType, String host, GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getAuthCode(appId, appType, host, new IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(String success) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(success));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void configApplication(String appId, int appType, long timestamp, String nonceStr, String signature, GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.configApplication(appId, appType, timestamp, nonceStr, signature, new IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return ??????????????? - ????????????
     */
    public long getServerDeltaTime() {
        if (!checkRemoteService()) {
            return 0L;
        }

        try {
            return mClient.getServerDeltaTime();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public String getImageThumbPara() {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getImageThumbPara();
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?????????????????????
     *
     * @param conversation
     * @return
     */
    public int getMessageCount(Conversation conversation) {
        if (!checkRemoteService()) {
            return 0;
        }

        try {
            return mClient.getMessageCount(conversation);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * ????????????????????????socks5?????????????????????????????????
     *
     * @param proxyInfo ????????????
     */
    public void setProxyInfo(Socks5ProxyInfo proxyInfo) {
        this.proxyInfo = proxyInfo;
        if (checkRemoteService()) {
            try {
                mClient.setProxyInfo(proxyInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    public boolean beginTransaction() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.begainTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ????????????
     */
    public void commitTransaction() {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.commitTransaction();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isCommercialServer() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isCommercialServer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
    ???????????????????????????????????????????????????
     */
    public boolean isReceiptEnabled() {
        if (!checkRemoteService()) {
            return false;
        }
        if (receiptStatus != -1) {
            return receiptStatus == 1;
        }

        try {
            boolean isReceiptEnabled = mClient.isReceiptEnabled();
            receiptStatus = isReceiptEnabled ? 1 : 0;
            return isReceiptEnabled;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isEnableSecretChat() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            return mClient.isEnableSecretChat();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    public boolean isUserEnableSecretChat() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            boolean disable = "1".equals(mClient.getUserSetting(UserSettingScope.DisableSecretChat, ""));
            return !disable;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param enable
     * @param callback
     */
    public void setUserEnableSecretChat(boolean enable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableSecretChat, "", enable ? "0" : "1", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            callback.onSuccess();
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * IM??????????????????bind??????
     *
     * @return
     */
    public boolean isIMServiceConnected() {
        return mClient != null;
    }

    public void startLog() {
        Log.d(TAG, "startLog");
        startLog = true;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.startLog();
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    public void stopLog() {
        Log.d(TAG, "stopLog");
        startLog = false;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.stopLog();
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    public void setSendLogCommand(String sendLogCommand) {
        this.sendLogCommand = sendLogCommand;
    }

    /**
     * ?????????????????????
     *
     * @return ???????????????
     */
    public String getProtoRevision() {
        if (!checkRemoteService()) {
            return "";
        }

        try {
            return mClient.getProtoRevision();
        } catch (RemoteException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getLogPath() {
        return gContext.getCacheDir().getAbsolutePath() + "/log";
    }

    public List<String> getLogFilesPath() {
        List<String> paths = new ArrayList<>();
        String path = getLogPath();

        //??????path????????????????????????????????????wflog?????????
        File dir = new File(path);
        File[] subFile = dir.listFiles();
        if (subFile != null) {
            for (File file : subFile) {
                //wflog???ChatService?????????????????????????????????????????????
                if (file.isFile() && file.getName().startsWith("wflog_")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
        return paths;
    }

    /**
     * ???????????????????????????token
     *
     * @param token
     * @param pushType ???????????????????????????????????????{@link cn.wildfirechat.push.PushService.PushServiceType}
     */
    public void setDeviceToken(String token, int pushType) {
        Log.d(TAG, "setDeviceToken " + token + " " + pushType);
        deviceToken = token;
        this.pushType = pushType;
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setDeviceToken(token, pushType);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @return
     */
    public boolean isGlobalSilent() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.GlobalSilent, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????????
     *
     * @param isSilent
     * @param callback
     */
    public void setGlobalSilent(boolean isSilent, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.GlobalSilent, "", isSilent ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @return
     */
    public boolean isVoipSilent() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.VoipSilent, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ????????????????????????????????????
     *
     * @param isSilent
     * @param callback
     */
    public void setVoipSilent(boolean isSilent, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.VoipSilent, "", isSilent ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @return
     */
    public boolean isDisableSyncDraft() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.DisableSyncDraft, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param isEnable
     * @param callback
     */
    public void setDisableSyncDraft(boolean isEnable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableSyncDraft, "", isEnable ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @return
     */
    public boolean isGlobalDisableSyncDraft() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return mClient.isGlobalDisableSyncDraft();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    public boolean isNoDisturbing() {
        CountDownLatch count = new CountDownLatch(1);
        boolean[] results = {false};
        getNoDisturbingTimes((isNoDisturbing, startMins, endMins) -> {
            int nowMin = ((int) (System.currentTimeMillis() / 1000 / 60)) % (24 * 60);
            if (isNoDisturbing) {
                if (endMins > startMins) {
                    if (nowMin > startMins && nowMin < endMins) {
                        results[0] = true;
                    }
                } else {
                    if (nowMin > startMins || nowMin < endMins) {
                        results[0] = true;
                    }
                }
            }
            count.countDown();
        });
        try {
            count.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results[0];
    }

    /**
     * ???????????????????????????
     */
    public interface GetNoDisturbingTimesCallback {
        /**
         * ?????????????????????
         *
         * @param isNoDisturbing ???????????????????????????
         * @param startMins      ???????????????UTC??????0????????????????????????????????????????????????????????????
         * @param endMins        ???????????????UTC??????0??????????????????????????????????????????????????????????????????????????????????????????????????????
         */
        void onResult(boolean isNoDisturbing, int startMins, int endMins);
    }

    /**
     * ????????????????????????
     *
     * @param callback ???????????????
     */
    public void getNoDisturbingTimes(GetNoDisturbingTimesCallback callback) {
        if (!checkRemoteService()) {
            callback.onResult(false, 0, 0);
            return;
        }

        try {
            String value = mClient.getUserSetting(UserSettingScope.NoDisturbing, "");
            if (!TextUtils.isEmpty(value)) {
                String[] arrs = value.split("\\|");
                if (arrs.length == 2) {
                    int start = Integer.parseInt(arrs[0]);
                    int end = Integer.parseInt(arrs[1]);
                    callback.onResult(true, start, end);
                    return;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        callback.onResult(false, 0, 0);
    }

    /**
     * ????????????????????????
     *
     * @param startMins ???????????????UTC??????0????????????????????????????????????????????????????????????
     * @param endMins   ???????????????UTC??????0??????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param callback  ??????????????????
     */
    public void setNoDisturbingTimes(int startMins, int endMins, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.NoDisturbing, "", startMins + "|" + endMins, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????
     *
     * @param callback ??????????????????
     */
    public void clearNoDisturbingTimes(final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.NoDisturbing, "", "", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????????????????
     *
     * @return
     */
    public boolean isHiddenNotificationDetail() {
        if (!checkRemoteService()) {
            return false;
        }

        try {
            return "1".equals(mClient.getUserSetting(UserSettingScope.HiddenNotificationDetail, ""));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ????????????????????????
     *
     * @param isHidden
     * @param callback
     */
    public void setHiddenNotificationDetail(boolean isHidden, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.HiddenNotificationDetail, "", isHidden ? "1" : "0", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess());
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @return
     */
    public boolean isUserEnableReceipt() {
        if (!checkRemoteService()) {
            return false;
        }
        if (userReceiptStatus != -1) {
            return userReceiptStatus == 1;
        }

        try {
            boolean disable = "1".equals(mClient.getUserSetting(UserSettingScope.DisableReceipt, ""));
            userReceiptStatus = disable ? 0 : 1;
            return !disable;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param enable
     * @param callback
     */
    public void setUserEnableReceipt(boolean enable, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null) {
                callback.onFail(ErrorCode.SERVICE_DIED);
            }
            return;
        }

        try {
            mClient.setUserSetting(UserSettingScope.DisableReceipt, "", enable ? "0" : "1", new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> {
                            userReceiptStatus = enable ? 1 : 0;
                            callback.onSuccess();
                        });
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param messageUid ?????????Uid
     * @param mediaType  ????????????
     * @param mediaPath  ???????????????
     * @param callback   ?????????????????????????????????
     */
    public void getAuthorizedMediaUrl(long messageUid, MessageContentMediaType mediaType, String mediaPath, final GetAuthorizedMediaUrlCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getAuthorizedMediaUrl(messageUid, mediaType.getValue(), mediaPath, new IGetAuthorizedMediaUrlCallback.Stub() {
                @Override
                public void onSuccess(String authorizedUrl, String backupUrl) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(authorizedUrl, backupUrl);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    /*
    ??????????????????????????????????????????????????????????????????????????????????????????getUploadUrl???????????????????????????????????????????????????
     */
    public boolean isSupportBigFilesUpload() {
        if (!checkRemoteService()) {
            return false;
        }
        try {
            boolean isSupportBigFilesUpload = mClient.isSupportBigFilesUpload();
            return isSupportBigFilesUpload;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????isSupportBigFilesUpload????????????????????????????????????
     *
     * @param fileName    ?????????
     * @param mediaType   ????????????
     * @param contentType Http???ContentType Header????????????????????????????????????"application/octet-stream"
     * @param callback    ??????????????????
     */
    public void getUploadUrl(String fileName, MessageContentMediaType mediaType, String contentType, GetUploadUrlCallback callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.getUploadUrl(fileName, mediaType.ordinal(), contentType, new IGetUploadUrlCallback.Stub() {
                @Override
                public void onSuccess(String uploadUrl, String remoteUrl, String backupUploadUrl, int type) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(uploadUrl, remoteUrl, backupUploadUrl, type));
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFail(errorCode));
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }

    }

    /*
    ??????PC????????????????????????PC??????Web?????????????????????
     */
    public List<PCOnlineInfo> getPCOnlineInfos() {
        String pcOnline = getUserSetting(UserSettingScope.PCOnline, "PC");
        String webOnline = getUserSetting(UserSettingScope.PCOnline, "Web");
        String wxOnline = getUserSetting(UserSettingScope.PCOnline, "WX");
        String padOnline = getUserSetting(UserSettingScope.PCOnline, "Pad");

        List<PCOnlineInfo> infos = new ArrayList<>();
        PCOnlineInfo info = PCOnlineInfo.infoFromStr(pcOnline, PCOnlineInfo.PCOnlineType.PC_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(webOnline, PCOnlineInfo.PCOnlineType.Web_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(wxOnline, PCOnlineInfo.PCOnlineType.WX_Online);
        if (info != null) {
            infos.add(info);
        }
        info = PCOnlineInfo.infoFromStr(padOnline, PCOnlineInfo.PCOnlineType.Pad_Online);
        if (info != null) {
            infos.add(info);
        }

        return infos;
    }

    /**
     * ??????PC????????????????????????PC??????Web??????????????????)
     *
     * @param pcClientId ????????????ID
     * @param callback   ????????????
     **/
    public void kickoffPCClient(String pcClientId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            mClient.kickoffPCClient(pcClientId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(callback::onSuccess);
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????PC?????????????????????????????????
     *
     * @return ??????true????????????PC????????????pc??????web????????????????????????????????????????????????????????????
     **/
    public boolean isMuteNotificationWhenPcOnline() {
        if (!checkRemoteService()) {
            return false;
        }

        String value = getUserSetting(UserSettingScope.MuteWhenPcOnline, "");
        if (value == null || !value.equals("1")) {
            return defaultSilentWhenPCOnline;
        }
        return !defaultSilentWhenPCOnline;
    }

    /**
     * ??????PC/Web???????????????????????????????????????????????????YES?????????IM????????????server.mobile_default_silent_when_pc_online ???false????????????????????????????????????false?????????????????????????????????
     *
     * @param defaultSilent ???????????????????????????
     */
    public void setDefaultSilentWhenPcOnline(boolean defaultSilent) {
        defaultSilentWhenPCOnline = defaultSilent;
    }

    /**
     * ???????????????PC???????????????????????????????????????
     *
     * @param isMute
     * @param callback
     **/
    public void muteNotificationWhenPcOnline(boolean isMute, GeneralCallback callback) {
        if (callback == null) {
            return;
        }
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        if (!defaultSilentWhenPCOnline) {
            isMute = !isMute;
        }
        setUserSetting(UserSettingScope.MuteWhenPcOnline, "", isMute ? "0" : "1", callback);
    }

    public void createSecretChat(String userId, final CreateSecretChatCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.createSecretChat(userId, new ICreateSecretChatCallback.Stub() {
                @Override
                public void onSuccess(String s, int i) throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess(s, i));
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void destroySecretChat(String targetId, final GeneralCallback callback) {
        if (!checkRemoteService()) {
            callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }
        try {
            mClient.destroySecretChat(targetId, new cn.wildfirechat.client.IGeneralCallback.Stub() {
                @Override
                public void onSuccess() throws RemoteException {
                    mainHandler.post(() -> callback.onSuccess());
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    mainHandler.post(() -> callback.onFail(errorCode));
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public SecretChatInfo getSecretChatInfo(String targetId) {
        if (!checkRemoteService()) {
            return null;
        }
        try {
            SecretChatInfo secretChatInfo = mClient.getSecretChatInfo(targetId);
            if (secretChatInfo == null) {
                removeConversation(new Conversation(Conversation.ConversationType.SecretChat, targetId, 0), true);
            }
            return secretChatInfo;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decodeSecretChatData(String targetid, byte[] mediaData) {
        if (!checkRemoteService()) {
            return new byte[0];
        }
        try {
            return mClient.decodeSecretChatData(targetid, mediaData);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param targetId
     * @param mediaData
     * @param callback
     */
    public void decodeSecretDataAsync(String targetId, byte[] mediaData, GeneralCallbackBytes callback) {
        if (!checkRemoteService()) {
            return;
        }
        MemoryFile memoryFile = null;
        try {
            memoryFile = new MemoryFile(targetId, mediaData.length);
            memoryFile.writeBytes(mediaData, 0, 0, memoryFile.length());
            FileDescriptor fileDescriptor = MemoryFileUtil.getFileDescriptor(memoryFile);
            ParcelFileDescriptor pdf = ParcelFileDescriptor.dup(fileDescriptor);
            MemoryFile finalMemoryFile = memoryFile;
            mClient.decodeSecretChatDataAsync(targetId, pdf, mediaData.length, new IGeneralCallbackInt.Stub() {
                @Override
                public void onSuccess(int length) throws RemoteException {
                    if (callback != null) {
                        // TODO ByteArrayOutputStream
                        byte[] data = new byte[length];
                        try {
                            finalMemoryFile.readBytes(data, 0, 0, length);
                            callback.onSuccess(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                            callback.onFail(-1);
                        } finally {
                            finalMemoryFile.close();
                        }
                    }
                }

                @Override
                public void onFailure(int errorCode) throws RemoteException {
                    if (callback != null) {
                        callback.onFail(errorCode);
                    }
                    finalMemoryFile.close();
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSecretChatBurnTime(String targetId, int burnTime) {
        if (!checkRemoteService()) {
            return;
        }

        try {
            mClient.setSecretChatBurnTime(targetId, burnTime);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public BurnMessageInfo getBurnMessageInfo(long messageId) {
        if (!checkRemoteService()) {
            return null;
        }

        try {
            return mClient.getBurnMessageInfo(messageId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendConferenceRequest(long sessionId, String roomId, String request, String data, final GeneralCallback2 callback) {
        sendConferenceRequest(sessionId, roomId, request, false, data, callback);
    }

    public void sendConferenceRequest(long sessionId, String roomId, String request, boolean advanced, String data, final GeneralCallback2 callback) {
        if (!checkRemoteService()) {
            if (callback != null)
                callback.onFail(ErrorCode.SERVICE_DIED);
            return;
        }

        try {
            Log.d("PCRTCClient", "send conference data:" + request + ": " + data);
            mClient.sendConferenceRequest(sessionId, roomId, request, advanced, data, new cn.wildfirechat.client.IGeneralCallback2.Stub() {
                @Override
                public void onSuccess(String result) throws RemoteException {
                    Log.d("PCRTCClient", "send conference result:" + result);
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(final int errorCode) throws RemoteException {
                    if (callback != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(errorCode);
                            }
                        });
                    }
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
            if (callback != null)
                mainHandler.post(() -> callback.onFail(ErrorCode.SERVICE_EXCEPTION));
        }
    }

    public MessageContent messageContentFromPayload(MessagePayload payload, String from) {

        MessageContent content = null;
        try {
            content = messageContentMap.get(payload.type).newInstance();
            if (content instanceof CompositeMessageContent) {
                ((CompositeMessageContent) content).decode(payload, this);
            } else {
                Log.e(TAG, "decode");
                content.decode(payload);
            }
            if (content instanceof NotificationMessageContent) {
                if (content instanceof RecallMessageContent) {
                    RecallMessageContent recallMessageContent = (RecallMessageContent) content;
                    if (recallMessageContent.getOperatorId().equals(userId)) {
                        ((NotificationMessageContent) content).fromSelf = true;
                    }
                } else if (from.equals(userId)) {
                    ((NotificationMessageContent) content).fromSelf = true;
                }
            }
            content.extra = payload.extra;
        } catch (Exception e) {
            android.util.Log.e(TAG, "decode message error, fallback to unknownMessageContent. " + payload.type);
            e.printStackTrace();
            if (content == null) {
                return null;
            }
            if (content.getPersistFlag() == PersistFlag.Persist || content.getPersistFlag() == PersistFlag.Persist_And_Count) {
                content = new UnknownMessageContent();
                ((UnknownMessageContent) content).setOrignalPayload(payload);
            } else {
                return null;
            }
        }
        return content;
    }

    private boolean checkRemoteService() {
        if (INST != null) {
            if (mClient != null) {
                return true;
            }

            Intent intent = new Intent(gContext, ClientService.class);
            intent.putExtra("clientId", getClientId());
            boolean result = gContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
            if (!result) {
                Log.e(TAG, "Bind service failure");
            }
        } else {
            Log.e(TAG, "Chat manager not initialized");
        }

        return false;
    }

    private void cleanLogFiles() {
        List<String> filePaths = ChatManager.Instance().getLogFilesPath();
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long LOG_KEEP_DURATION = 7 * 24 * 60 * 60 * 1000;
        for (String path : filePaths) {
            File file = new File(path);
            if (file.exists() && file.lastModified() > 0 && now - file.lastModified() > LOG_KEEP_DURATION) {
                file.deleteOnExit();
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "marsClientService connected");
            mClient = IRemoteClient.Stub.asInterface(iBinder);
            workHandler.post(() -> {
                try {
                    if (useSM4) {
                        mClient.useSM4();
                    }

                    mClient.setBackupAddressStrategy(backupAddressStrategy);
                    if (!TextUtils.isEmpty(backupAddressHost))
                        mClient.setBackupAddress(backupAddressHost, backupAddressPort);

                    if (proxyInfo != null) {
                        mClient.setProxyInfo(proxyInfo);
                    }
                    Log.d(TAG, "SERVER_HOST = " + SERVER_HOST);
                    mClient.setServerAddress(SERVER_HOST);
                    for (Class clazz : messageContentMap.values()) {
                        mClient.registerMessageContent(clazz.getName());
                    }

                    if (startLog) {
                        startLog();
                    } else {
                        stopLog();
                    }

                    if (!TextUtils.isEmpty(deviceToken)) {
                        mClient.setDeviceToken(deviceToken, pushType);
                    }

                    mClient.setForeground(1);
                    mClient.setOnReceiveMessageListener(new IOnReceiveMessageListener.Stub() {
                        @Override
                        public void onReceive(List<Message> messages, boolean hasMore) throws RemoteException {
                            onReceiveMessage(messages, hasMore);
                        }

                        @Override
                        public void onRecall(long messageUid) throws RemoteException {
                            onRecallMessage(messageUid);
                        }

                        @Override
                        public void onDelete(long messageUid) throws RemoteException {
                            onDeleteMessage(messageUid);
                        }

                        @Override
                        public void onDelivered(Map deliveryMap) throws RemoteException {
                            onMsgDelivered(deliveryMap);
                        }

                        @Override
                        public void onReaded(List<ReadEntry> readEntrys) throws RemoteException {
                            onMsgReaded(readEntrys);
                        }
                    });
                    mClient.setOnConnectionStatusChangeListener(new IOnConnectionStatusChangeListener.Stub() {
                        @Override
                        public void onConnectionStatusChange(int connectionStatus) throws RemoteException {
                            ChatManager.this.onConnectionStatusChange(connectionStatus);
                        }
                    });
                    mClient.setOnConnectToServerListener(new IOnConnectToServerListener.Stub() {
                        @Override
                        public void onConnectToServer(String host, String ip, int port) throws RemoteException {
                            ChatManager.this.onConnectToServer(host, ip, port);
                        }
                    });
                    mClient.setOnUserInfoUpdateListener(new IOnUserInfoUpdateListener.Stub() {
                        @Override
                        public void onUserInfoUpdated(List<UserInfo> userInfos) throws RemoteException {
                            ChatManager.this.onUserInfoUpdate(userInfos);
                        }
                    });
                    mClient.setOnGroupInfoUpdateListener(new IOnGroupInfoUpdateListener.Stub() {
                        @Override
                        public void onGroupInfoUpdated(List<GroupInfo> groupInfos) throws RemoteException {
                            ChatManager.this.onGroupInfoUpdated(groupInfos);
                        }
                    });
                    mClient.setOnGroupMembersUpdateListener(new IOnGroupMembersUpdateListener.Stub() {
                        @Override
                        public void onGroupMembersUpdated(String groupId, List<GroupMember> members) throws RemoteException {
                            ChatManager.this.onGroupMembersUpdate(groupId, members);
                        }
                    });
                    mClient.setOnFriendUpdateListener(new IOnFriendUpdateListener.Stub() {
                        @Override
                        public void onFriendListUpdated(List<String> friendList) throws RemoteException {
                            ChatManager.this.onFriendListUpdated(friendList);
                        }

                        @Override
                        public void onFriendRequestUpdated(List<String> newRequests) throws RemoteException {
                            ChatManager.this.onFriendReqeustUpdated(newRequests);
                        }
                    });
                    mClient.setOnSettingUpdateListener(new IOnSettingUpdateListener.Stub() {
                        @Override
                        public void onSettingUpdated() throws RemoteException {
                            ChatManager.this.onSettingUpdated();
                        }
                    });
                    mClient.setOnChannelInfoUpdateListener(new IOnChannelInfoUpdateListener.Stub() {
                        @Override
                        public void onChannelInfoUpdated(List<ChannelInfo> channelInfos) throws RemoteException {
                            ChatManager.this.onChannelInfoUpdate(channelInfos);
                        }
                    });
                    mClient.setOnConferenceEventListener(new IOnConferenceEventListener.Stub() {
                        @Override
                        public void onConferenceEvent(String event) throws RemoteException {
                            ChatManager.this.onConferenceEvent(event);
                        }
                    });
                    mClient.setOnTrafficDataListener(new IOnTrafficDataListener.Stub() {
                        @Override
                        public void onTrafficData(long send, long recv) throws RemoteException {
                            ChatManager.this.onTrafficData(send, recv);
                        }
                    });

                    mClient.setUserOnlineEventListener(new IOnUserOnlineEventListener.Stub() {

                        @Override
                        public void onUserOnlineEvent(UserOnlineState[] states) throws RemoteException {
                            ChatManager.this.onUserOnlineEvent(states);
                        }
                    });

                    mClient.setSecretChatStateChangedListener(new IOnSecretChatStateListener.Stub() {
                        @Override
                        public void onSecretChatStateChanged(String targetid, int state) throws RemoteException {
                            ChatManager.this.onSecretChatStateChanged(targetid, state);
                        }
                    });


                    mClient.setSecretMessageBurnStateListener(new IOnSecretMessageBurnStateListener.Stub() {
                        @Override
                        public void onSecretMessageStartBurning(String targetId, long playedMsgId) throws RemoteException {
                            ChatManager.this.onSecretMessageStartBurning(targetId, playedMsgId);
                        }

                        @Override
                        public void onSecretMessageBurned(int[] messageIds) throws RemoteException {
                            if (messageIds != null && messageIds.length > 0) {
                                List<Long> arr = new ArrayList<>();
                                for (int i = 0; i < messageIds.length; i++) {
                                    arr.add((long) messageIds[i]);
                                }
                                ChatManager.this.onSecretMessageBurned(arr);
                            }
                        }
                    });

                    mClient.setLiteMode(isLiteMode);
                    mClient.setLowBPSMode(isLowBPSMode);

                    if (!TextUtils.isEmpty(protoUserAgent)) {
                        mClient.setProtoUserAgent(protoUserAgent);
                    }
                    if (!protoHttpHeaderMap.isEmpty()) {
                        for (Map.Entry<String, String> entry : protoHttpHeaderMap.entrySet()) {
                            try {
                                mClient.addHttpHeader(entry.getKey(), entry.getValue());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(token)) {
                        mClient.connect(userId, token);
                    }

                    int clientConnectionStatus = mClient.getConnectionStatus();
                    if (connectionStatus == ConnectionStatus.ConnectionStatusConnected) {
                        onConnectionStatusChange(clientConnectionStatus);
                    }

                    mainHandler.post(() -> {
                        for (IMServiceStatusListener listener : imServiceStatusListeners) {
                            listener.onServiceConnected();
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "marsClientService ServiceDisconnected");
            mClient = null;
            checkRemoteService();
            mainHandler.post(() -> {
                for (IMServiceStatusListener listener : imServiceStatusListeners) {
                    listener.onServiceDisconnected();
                }
            });
        }
    };

    private static interface MediaMessageUploadCallback {
        void onMediaMessageUploaded(String remoteUrl);
    }

    private void registerCoreMessageContents() {
        registerMessageContent(AddGroupMemberNotificationContent.class);
        registerMessageContent(CallStartMessageContent.class);
        registerMessageContent(ConferenceInviteMessageContent.class);
        registerMessageContent(ChangeGroupNameNotificationContent.class);
        registerMessageContent(ChangeGroupPortraitNotificationContent.class);
        registerMessageContent(CreateGroupNotificationContent.class);
        registerMessageContent(DismissGroupNotificationContent.class);
        registerMessageContent(FileMessageContent.class);
        registerMessageContent(ImageMessageContent.class);
        registerMessageContent(LinkMessageContent.class);
        registerMessageContent(KickoffGroupMemberNotificationContent.class);
        registerMessageContent(LocationMessageContent.class);
        registerMessageContent(ModifyGroupAliasNotificationContent.class);
        registerMessageContent(ModifyGroupExtraNotificationContent.class);
        registerMessageContent(ModifyGroupMemberExtraNotificationContent.class);
        registerMessageContent(QuitGroupNotificationContent.class);
        registerMessageContent(RecallMessageContent.class);
        registerMessageContent(DeleteMessageContent.class);
        registerMessageContent(SoundMessageContent.class);
        registerMessageContent(StickerMessageContent.class);
        registerMessageContent(TextMessageContent.class);
        registerMessageContent(PCLoginRequestMessageContent.class);
        registerMessageContent(PTextMessageContent.class);
        registerMessageContent(TipNotificationContent.class);
        registerMessageContent(FriendAddedMessageContent.class);
        registerMessageContent(FriendGreetingMessageContent.class);
        registerMessageContent(TransferGroupOwnerNotificationContent.class);
        registerMessageContent(VideoMessageContent.class);
        registerMessageContent(TypingMessageContent.class);
        registerMessageContent(GroupMuteNotificationContent.class);
        registerMessageContent(GroupJoinTypeNotificationContent.class);
        registerMessageContent(GroupPrivateChatNotificationContent.class);
        registerMessageContent(GroupSetManagerNotificationContent.class);
        registerMessageContent(GroupMuteMemberNotificationContent.class);
        registerMessageContent(GroupAllowMemberNotificationContent.class);
        registerMessageContent(KickoffGroupMemberVisibleNotificationContent.class);
        registerMessageContent(QuitGroupVisibleNotificationContent.class);
        registerMessageContent(CardMessageContent.class);
        registerMessageContent(CompositeMessageContent.class);
        registerMessageContent(MarkUnreadMessageContent.class);
        registerMessageContent(PTTSoundMessageContent.class);
        registerMessageContent(StartSecretChatMessageContent.class);
        registerMessageContent(EnterChannelChatMessageContent.class);
        registerMessageContent(LeaveChannelChatMessageContent.class);
        registerMessageContent(MultiCallOngoingMessageContent.class);
        registerMessageContent(JoinCallRequestMessageContent.class);
        registerMessageContent(RichNotificationMessageContent.class);
        registerMessageContent(ArticlesMessageContent.class);
        registerMessageContent(ChannelMenuEventMessageContent.class);
        registerMessageContent(SensitiveWordMessageContent.class);
        registerMessageContent(SilenceMessageContent.class);
    }

    private MessageContent contentOfType(int type) {
        Class<? extends MessageContent> cls = messageContentMap.get(type);
        if (cls != null) {
            try {
                return cls.newInstance();
            } catch (Exception e) {
                android.util.Log.e(TAG, "create message content instance failed, fall back to UnknownMessageContent, the message content class must have a default constructor. " + type);
                e.printStackTrace();
            }
        }
        return new UnknownMessageContent();
    }

    private static int[] convertIntegers(List<Integer> integers) {
        if (integers == null) {
            return new int[0];
        }

        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    private static boolean checkSDKHost(String host) {
        Class clazz;
        Method method;
        boolean result;
        try {
            Log.d(TAG, "*************** SDK?????? *****************");
            clazz = Class.forName("cn.wildfirechat.avenginekit.AVEngineKit");
            method = clazz.getMethod("isSupportMultiCall");
            boolean multiCall = (boolean) method.invoke(null);
            method = clazz.getMethod("isSupportConference");
            boolean conference = (boolean) method.invoke(null);
            if (conference) {
                Log.d(TAG, "?????????SDK????????????");
            } else if (multiCall) {
                Log.d(TAG, "?????????SDK????????????");
            } else {
                Log.d(TAG, "?????????SDK????????????");
            }

            clazz = Class.forName("cn.wildfirechat.moment.MomentClient");
            method = clazz.getMethod("checkAddress", String.class);
            result = (boolean) method.invoke(null, host);
            if (!result) {
                Log.d(TAG, "??????????????????SDK??????????????????????????????SDK??????????????????????????????????????????????????????");
            }

            clazz = Class.forName("cn.wildfirechat.ptt.PTTClient");
            method = clazz.getMethod("checkAddress", String.class);
            result = (boolean) method.invoke(null, host);
            if (!result) {
                Log.d(TAG, "???????????????SDK??????????????????????????????SDK??????????????????????????????????????????????????????");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "*************** SDK?????? *****************");
        }
        return true;
    }
}
