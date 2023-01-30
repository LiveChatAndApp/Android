/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickContactFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class NewRemoveGroupMemberFragment extends PickContactFragment {
    private GroupInfo groupInfo;
    private ArrayList<UserInfo> userInfoList = new ArrayList<>();

    public static NewRemoveGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        NewRemoveGroupMemberFragment fragment = new NewRemoveGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static NewRemoveGroupMemberFragment newInstance(ArrayList<UserInfo> array) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("userInfos", array);
        NewRemoveGroupMemberFragment fragment = new NewRemoveGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
        ArrayList<UserInfo> array = getArguments().getParcelableArrayList("userInfos");
        if (array != null) {
            userInfoList.addAll(array);
        }
    }


    @Override
    protected void setupPickFromUsers() {
        if (groupInfo == null) {
            showContent();
            PickUserViewModel pickUserViewModel = new ViewModelProvider(getActivity()).get(PickUserViewModel.class);
            List<UIUserInfo> uiUserInfos = UIUserInfo.fromUserInfos(userInfoList);

            List<String> memberIds = new ArrayList<>(uiUserInfos.size());
            memberIds.add(ChatManager.Instance().getUserId());
            pickUserViewModel.setUncheckableIds(memberIds);

            pickUserViewModel.setUsers(uiUserInfos);
            userListAdapter.setUsers(uiUserInfos);
        }
    }

    @Override
    public void initHeaderViewHolders() {
        // do nothing
    }
}
