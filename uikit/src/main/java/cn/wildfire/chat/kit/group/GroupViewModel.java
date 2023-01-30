/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfire.chat.kit.group.page.GroupDataSourceFactory;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.utils.PinyinUtils;
import cn.wildfire.chat.kit.utils.portrait.CombineBitmapTools;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.notification.NotificationMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupPageInfo;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.WebResponse;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GeneralCallback2;
import cn.wildfirechat.remote.GetConversationListCallback;
import cn.wildfirechat.remote.GetGroupsCallback;
import cn.wildfirechat.remote.GetRemoteMessageCallback;
import cn.wildfirechat.remote.OnGroupInfoUpdateListener;
import cn.wildfirechat.remote.OnGroupMembersUpdateListener;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupViewModel extends ViewModel implements OnGroupInfoUpdateListener, OnGroupMembersUpdateListener {
    private MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData;
    private MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData;
    private LiveData<PagedList<GroupPageInfo.Item>> data;

    public GroupViewModel() {
        super();
        ChatManager.Instance().addGroupInfoUpdateListener(this);
        ChatManager.Instance().addGroupMembersUpdateListener(this);
    }

    @Override
    protected void onCleared() {
        ChatManager.Instance().removeGroupInfoUpdateListener(this);
        ChatManager.Instance().removeGroupMembersUpdateListener(this);
    }

    public MutableLiveData<List<GroupInfo>> groupInfoUpdateLiveData() {
        if (groupInfoUpdateLiveData == null) {
            groupInfoUpdateLiveData = new MutableLiveData<>();
        }
        return groupInfoUpdateLiveData;
    }

    public MutableLiveData<List<GroupMember>> groupMembersUpdateLiveData() {
        if (groupMembersUpdateLiveData == null) {
            groupMembersUpdateLiveData = new MutableLiveData<>();
        }
        return groupMembersUpdateLiveData;
    }

    public MutableLiveData<List<UIUserInfo>> getGroupMemberUIUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> groupMemberLiveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            List<String> memberIds = new ArrayList<>(members.size());
            for (GroupMember member : members) {
                memberIds.add(member.memberId);
            }
            List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
            List<UIUserInfo> users = UIUserInfo.fromUserInfos(userInfos);
            groupMemberLiveData.postValue(users);
        });
        return groupMemberLiveData;
    }

    public MutableLiveData<List<UserInfo>> getGroupMemberUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UserInfo>> groupMemberLiveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            List<String> memberIds = new ArrayList<>(members.size());
            for (GroupMember member : members) {
                memberIds.add(member.memberId);
            }
            List<UserInfo> userInfos = ChatManager.Instance().getUserInfos(memberIds, groupId);
            groupMemberLiveData.postValue(userInfos);
        });
        return groupMemberLiveData;
    }

    @Override
    public void onGroupInfoUpdate(List<GroupInfo> groupInfos) {
        if (groupInfoUpdateLiveData != null) {
            groupInfoUpdateLiveData.setValue(groupInfos);
        }
    }

    public MutableLiveData<OperateResult<String>> createGroup(Context context, List<UserInfo> checkedUsers, MessageContent notifyMsg, List<Integer> lines) {
        List<String> selectedIds = new ArrayList<>(checkedUsers.size());
        List<UserInfo> selectedUsers = new ArrayList<>();
        for (UserInfo userInfo : checkedUsers) {
            selectedIds.add(userInfo.uid);
            selectedUsers.add(userInfo);
        }
        String id = ChatManager.Instance().getUserId();
        if (!selectedIds.contains(id)) {
            selectedIds.add(id);
            selectedUsers.add(ChatManager.Instance().getUserInfo(id, false));
        }
        String groupName = "";
        for (int i = 0; i < 3 && i < selectedUsers.size(); i++) {
            groupName += selectedUsers.get(i).displayName + "、";
        }
        groupName = groupName.substring(0, groupName.length() - 1);
        if (selectedUsers.size() > 3) {
            groupName += " ...";
        }

        groupName = groupName.substring(0, groupName.length() - 1);

        MutableLiveData<OperateResult<String>> groupLiveData = new MutableLiveData<>();
        String finalGroupName = groupName;
        ChatManager.Instance().createGroup(null, finalGroupName, null, GroupInfo.GroupType.Restricted, null, selectedIds, null, lines, notifyMsg, new GeneralCallback2() {
            @Override
            public void onSuccess(String groupId) {
                if (TextUtils.isEmpty(groupId)) {
                    groupLiveData.setValue(new OperateResult<>(256));
                } else {
                    groupLiveData.setValue(new OperateResult<>(groupId, 0));
                }
            }

            @Override
            public void onFail(int errorCode) {
                groupLiveData.setValue(new OperateResult<>(errorCode));
            }
        });
        return groupLiveData;
    }

    public MutableLiveData<OperateResult<String>> createGroup(String groupName, String portrait, List<UserInfo> checkedUsers, MessageContent notifyMsg, List<Integer> lines) {
        List<String> selectedIds = new ArrayList<>(checkedUsers.size());
        List<UserInfo> selectedUsers = new ArrayList<>();
        for (UserInfo userInfo : checkedUsers) {
            selectedIds.add(userInfo.uid);
            selectedUsers.add(userInfo);
        }
        String id = ChatManager.Instance().getUserId();
        if (!selectedIds.contains(id)) {
            selectedIds.add(id);
            selectedUsers.add(ChatManager.Instance().getUserInfo(id, false));
        }

        MutableLiveData<OperateResult<String>> groupLiveData = new MutableLiveData<>();
        ChatManager.Instance().createGroup(null, groupName, portrait, GroupInfo.GroupType.Restricted, null, selectedIds, null, lines, notifyMsg, new GeneralCallback2() {
            @Override
            public void onSuccess(String groupId) {
                if (TextUtils.isEmpty(groupId)) {
                    groupLiveData.setValue(new OperateResult<>(256));
                } else {
                    groupLiveData.setValue(new OperateResult<>(groupId, 0));
                }
            }

            @Override
            public void onFail(int errorCode) {
                groupLiveData.setValue(new OperateResult<>(errorCode));
            }
        });
        return groupLiveData;
    }

    public MutableLiveData<Boolean> addGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        // TODO need update group portrait or not?
        ChatManager.Instance().addGroupMembers(groupInfo.target, memberIds, null, notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });

        return result;
    }

    public MutableLiveData<Boolean> removeGroupMember(GroupInfo groupInfo, List<String> memberIds, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManagerHolder.gChatManager.removeGroupMembers(groupInfo.target, memberIds, notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });

        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupManager(String groupId, boolean isSet, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setGroupManager(groupId, isSet, memberIds, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> muteGroupMember(String groupId, boolean mute, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().muteGroupMember(groupId, mute, memberIds, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> allowGroupMember(String groupId, boolean allow, List<String> memberIds, NotificationMessageContent notifyMsg, List<Integer> lines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().allowGroupMember(groupId, allow, memberIds, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> muteAll(String groupId, boolean mute, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Mute, mute ? "1" : "0", notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> enablePrivateChat(String groupId, boolean enablePrivateChat, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_PrivateChat, enablePrivateChat ? "0" : "1", notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupJoinType(String groupId, int joinType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_JoinType, joinType + "", notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupSearchType(String groupId, int searchType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_Searchable, searchType + "", notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupHistoryMessage(String groupId, int searchType, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, ModifyGroupInfoType.Modify_Group_History_Message, searchType + "", notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public @Nullable
    GroupInfo getGroupInfo(String groupId, boolean refresh) {
        return ChatManager.Instance().getGroupInfo(groupId, refresh);
    }

    public List<GroupMember> getGroupMembers(String groupId, boolean forceRefresh) {
        return ChatManager.Instance().getGroupMembers(groupId, forceRefresh);
    }

    public MutableLiveData<List<GroupMember>> getGroupMembersLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<GroupMember>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, refresh);
            data.postValue(members);
        });

        return data;
    }

    public MutableLiveData<List<UIUserInfo>> getGroupManagerUIUserInfosLiveData(String groupId, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> managers = getGroupManagers(groupId);
            List<UIUserInfo> userInfos = managerMemberToUIUserInfo(groupId, managers);
            data.postValue(userInfos);
        });

        return data;
    }

    public MutableLiveData<List<UIUserInfo>> getMutedOrAllowedMemberUIUserInfosLiveData(String groupId, boolean muted, boolean refresh) {
        MutableLiveData<List<UIUserInfo>> data = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<GroupMember> mutedMembers = getMutedOrAllowedMembers(groupId, muted);
            List<UIUserInfo> userInfos = mutedOrAllowedMemberToUIUserInfo(groupId, muted, mutedMembers);
            data.postValue(userInfos);
        });

        return data;
    }

    public List<GroupMember> getGroupManagers(String groupId) {
        List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, false);
        List<GroupMember> managers = new ArrayList<>();
        if (members != null) {
            for (GroupMember member : members) {
                if (member.type == GroupMember.GroupMemberType.Manager || member.type == GroupMember.GroupMemberType.Owner) {
                    managers.add(member);
                }
            }
        }
        return managers;
    }

    public List<String> getGroupManagerIds(String groupId) {
        List<GroupMember> managers = getGroupManagers(groupId);
        List<String> mangerIds = new ArrayList<>();
        if (managers != null) {
            for (GroupMember manager : managers) {
                mangerIds.add(manager.memberId);
            }
        }
        return mangerIds;
    }


    public List<GroupMember> getMutedOrAllowedMembers(String groupId, boolean muted) {
        List<GroupMember> members = ChatManager.Instance().getGroupMembers(groupId, false);
        List<GroupMember> managers = new ArrayList<>();
        if (members != null) {
            for (GroupMember member : members) {
                if ((muted && member.type == GroupMember.GroupMemberType.Allowed)
                        || !muted && member.type == GroupMember.GroupMemberType.Muted) {
                    managers.add(member);
                }
            }
        }
        return managers;
    }


    public List<String> getMutedOrAllowedMemberIds(String groupId, boolean muted) {
        List<GroupMember> mutedMembers = getMutedOrAllowedMembers(groupId, muted);
        List<String> mutedIds = new ArrayList<>();
        if (mutedMembers != null) {
            for (GroupMember manager : mutedMembers) {
                mutedIds.add(manager.memberId);
            }
        }
        return mutedIds;

    }

    private List<UIUserInfo> managerMemberToUIUserInfo(String groupId, List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupId);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            String name = ChatManager.Instance().getGroupMemberDisplayName(userInfo);
            if (!TextUtils.isEmpty(name)) {
                String pinyin = PinyinUtils.getPinyin(name);
                char c = pinyin.toUpperCase().charAt(0);
                if (c >= 'A' && c <= 'Z') {
                    info.setSortName(pinyin);
                } else {
                    // 为了让排序排到最后
                    info.setSortName("{" + pinyin);
                }
            } else {
                info.setSortName("");
            }

            for (GroupMember member : members) {
                if (userInfo.uid.equals(member.memberId)) {
                    if (member.type == GroupMember.GroupMemberType.Manager) {
                        info.setCategory("管理员");
                        if (!showManagerCategory) {
                            showManagerCategory = true;
                            info.setShowCategory(true);
                        }
                        uiUserInfos.add(info);
                    } else {
                        info.setCategory("群主");
                        info.setShowCategory(true);
                        uiUserInfos.add(0, info);
                    }
                    break;
                }
            }
        }
        return uiUserInfos;
    }

    private List<UIUserInfo> mutedOrAllowedMemberToUIUserInfo(String groupId, boolean muted, List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return null;
        }

        List<String> memberIds = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            memberIds.add(member.memberId);
        }

        List<UIUserInfo> uiUserInfos = new ArrayList<>();
        List<UserInfo> userInfos = UserViewModel.getUsers(memberIds, groupId);
        boolean showManagerCategory = false;
        for (UserInfo userInfo : userInfos) {
            UIUserInfo info = new UIUserInfo(userInfo);
            String name = ChatManager.Instance().getGroupMemberDisplayName(userInfo);
            if (!TextUtils.isEmpty(name)) {
                String pinyin = PinyinUtils.getPinyin(name);
                char c = pinyin.toUpperCase().charAt(0);
                if (c >= 'A' && c <= 'Z') {
                    info.setSortName(pinyin);
                } else {
                    // 为了让排序排到最后
                    info.setSortName("{" + pinyin);
                }
            } else {
                info.setSortName("");
            }
            info.setCategory(muted ? "白名单列表" : "禁言列表");
            if (!showManagerCategory) {
                showManagerCategory = true;
                info.setShowCategory(true);
            }
            uiUserInfos.add(info);
        }
        return uiUserInfos;
    }

    public GroupMember getGroupMember(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMember(groupId, memberId);
    }

    public String getGroupMemberDisplayName(String groupId, String memberId) {
        return ChatManager.Instance().getGroupMemberDisplayName(groupId, memberId);
    }

    public MutableLiveData<OperateResult<List<GroupInfo>>> getFavGroups() {
        MutableLiveData<OperateResult<List<GroupInfo>>> result = new MutableLiveData<>();
        ChatManager.Instance().getFavGroups(new GetGroupsCallback() {
            @Override
            public void onSuccess(List<GroupInfo> groupInfos) {
                result.setValue(new OperateResult<>(groupInfos, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(null, 0));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> modifyGroupInfo(String groupId, ModifyGroupInfoType modifyType, String newValue, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupInfo(groupId, modifyType, newValue, notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setGroupRemark(String groupId, String remark) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setGroupRemark(groupId, remark, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult> modifyMyGroupAlias(String groupId, String alias, MessageContent notifyMsg, List<Integer> notifyLines) {
        MutableLiveData<OperateResult> result = new MutableLiveData<>();
        ChatManager.Instance().modifyGroupAlias(groupId, alias, notifyLines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<OperateResult<Boolean>> setFavGroup(String groupId, boolean fav) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().setFavGroup(groupId, fav, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));
            }
        });
        return result;
    }

    public MutableLiveData<Boolean> quitGroup(String groupId, List<Integer> lines, MessageContent notifyMsg) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().quitGroup(groupId, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

    public MutableLiveData<Boolean> dismissGroup(String groupId, List<Integer> lines, MessageContent notifyMsg) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        ChatManager.Instance().dismissGroup(groupId, lines, notifyMsg, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(true);
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(false);
            }
        });
        return result;
    }

    private @Nullable
    String generateGroupPortrait(Context context, List<UserInfo> userInfos) throws Exception {
        List<Bitmap> bitmaps = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            Drawable drawable;
            try {
                drawable = GlideUtil.load(context, userInfo.portrait).placeholder(R.mipmap.avatar_def).submit(60, 60).get();
            } catch (Exception e) {
                e.printStackTrace();
                drawable = GlideUtil.load(context, R.mipmap.avatar_def).submit(60, 60).get();
            }
            if (drawable instanceof BitmapDrawable) {
                bitmaps.add(((BitmapDrawable) drawable).getBitmap());
            }
        }
        Bitmap bitmap = CombineBitmapTools.combimeBitmap(context, 60, 60, bitmaps);
        if (bitmap == null) {
            return null;
        }
        //create a file to write bitmap data
        File f = new File(context.getCacheDir(), System.currentTimeMillis() + ".png");
        f.createNewFile();

        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapData = bos.toByteArray();

        //write the bytes in file
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        return f.getAbsolutePath();
    }

    @Override
    public void onGroupMembersUpdate(String groupId, List<GroupMember> groupMembers) {
        if (groupMembersUpdateLiveData != null && groupMembers != null && !groupMembers.isEmpty()) {
            groupMembersUpdateLiveData.setValue(groupMembers);
        }
    }

    public LiveData<Bitmap> getGroupImage(Context context, List<UserInfo> userInfos) {
        MutableLiveData<Bitmap> liveData = new MutableLiveData<>();
        ChatManager.Instance().getWorkHandler().post(() -> {
            // 群组头像
            Bitmap portrait = ImageUtils.generateNewGroupPortrait(context, userInfos, 60);
            liveData.postValue(portrait);
        });
        return liveData;
    }

    public LiveData<WebResponse<String>> uploadGroupPortrait(File file) {
        MutableLiveData<WebResponse<String>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().uploadGroupPortrait(file, new SimpleCallback<WebResponse<String>>() {
            @Override
            public void onUiSuccess(WebResponse<String> response) {
                liveData.postValue(response);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WebResponse<String> response = new WebResponse<>(code, msg);
                liveData.postValue(response);
            }
        });
        return liveData;
    }

    public LiveData<PagedList<GroupPageInfo.Item>> observerMyGroupList(Fragment fragment) {
        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(20) // 分页加载的数量
                .setEnablePlaceholders(true) // 当item为null是否使用PlaceHolder展示
                .setInitialLoadSizeHint(20) // 预加载的数量, 与分页加载的数量成倍数关系
                .build();
        data = new LivePagedListBuilder(new GroupDataSourceFactory(), config).build();//可以放在ViewModel层处理
        return data;
    }

    public void resetMyGroupList() {
        if (data == null)
            return;
        PagedList<GroupPageInfo.Item> pagedList = data.getValue();
        if (pagedList != null && pagedList.getDataSource() != null) {
            pagedList.getDataSource().invalidate();
        }
    }

    /**
     * @param groupType  类型
     * @param page       页数
     * @param pageOfSize 资料数
     */
    public LiveData<WebResponse<List<ConversationInfo>>> getGroupList(int groupType, int page, int pageOfSize) {
//        List<ConversationInfo> conversationInfos =
        MutableLiveData<WebResponse<List<ConversationInfo>>> liveData = new MutableLiveData<>();

        // 先讀取聊天列表
        ChatManager.Instance().getWorkHandler().post(() -> {
            ChatManager.Instance().getConversationListAsync(ConversationListFragment.types, ConversationListFragment.lines, new GetConversationListCallback() {
                @Override
                public void onSuccess(List<ConversationInfo> conversationInfos) {
                    // 取廣播列表，判斷要不要新增近本地端
                    ImplementUserSource.Instance().getGroupList(groupType, page, pageOfSize, new SimpleCallback<WebResponse<GroupPageInfo>>() {
                        @Override
                        public void onUiSuccess(WebResponse<GroupPageInfo> response) {
                            if (response.isSuccess() && response.result != null) {
                                // 把GroupPageInfo.Item 轉換為 ConversationInfo
                                List<GroupPageInfo.Item> resultItemList = response.result.data;
                                List<ConversationInfo> list = new ArrayList<>();
                                for (GroupPageInfo.Item item : resultItemList) {
                                    Conversation conversation = new Conversation();
                                    conversation.target = item.gid;
                                    conversation.type = Conversation.ConversationType.Group;
                                    ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
                                    list.add(conversationInfo);
                                }
                                // 判斷 server 廣播conversation列表，本地端有沒有缺
                                List<ConversationInfo> addList = new ArrayList<>();
                                for (ConversationInfo newInfo : list) {
                                    boolean isNew = true;
                                    for (ConversationInfo info : conversationInfos) {
                                        if (newInfo.conversation.target.equals(info.conversation.target)) {
                                            isNew = false;
                                        }
                                    }
                                    if (isNew) {
                                        // 广播群 预设 置顶
                                        ChatManager.Instance().setConversationTop(newInfo.conversation, 1);
                                        // 广播群 预设显示 成员匿称
                                        ChatManager.Instance().setUserSetting(UserSettingScope.GroupHideNickname, newInfo.conversation.target, "1", null);
                                        // 取廣播群的20筆訊息, 並儲存
                                        getConversationRemoteMessage(newInfo, new GetRemoteMessageCallback() {
                                            @Override
                                            public void onSuccess(List<Message> messages) {
                                                WebResponse<List<ConversationInfo>> newResponse = WebResponse.createSuccess();
                                                newResponse.result = new ArrayList<>();
                                                liveData.postValue(newResponse);
                                            }

                                            @Override
                                            public void onFail(int errorCode) {

                                            }
                                        });
                                        addList.add(newInfo);
                                    }
                                }
                                WebResponse<List<ConversationInfo>> newResponse = WebResponse.createSuccess();
                                newResponse.result = addList;
                                liveData.postValue(newResponse);
                            }
                        }

                        @Override
                        public void onUiFailure(int code, String msg) {
                            WebResponse<List<ConversationInfo>> response = new WebResponse<>();
                            response.code = code;
                            response.message = msg;
                            liveData.postValue(response);
                        }
                    });
                }

                @Override
                public void onFail(int errorCode) {

                }
            });
        });
        return liveData;
    }

    /**
     * 取廣播群的20筆訊息, 並儲存
     *
     * @param info
     */
    private void getConversationRemoteMessage(ConversationInfo info, GetRemoteMessageCallback callback) {
        ChatManager.Instance().getRemoteMessagesAndSave(info.conversation, null, 0, 20, callback);
    }
}
