/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickContactFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class AddGroupMemberFragment extends PickContactFragment {
    private GroupInfo groupInfo;
    private ArrayList<UserInfo> userInfoList = new ArrayList<>();

    public static AddGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        AddGroupMemberFragment fragment = new AddGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddGroupMemberFragment newInstance(ArrayList<UserInfo> array) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("userInfos", array);
        AddGroupMemberFragment fragment = new AddGroupMemberFragment();
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
        super.setupPickFromUsers();
        if (groupInfo != null) {
            PickUserViewModel pickUserViewModel = new ViewModelProvider(getActivity()).get(PickUserViewModel.class);

            GroupViewModel groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

            groupViewModel.getGroupMemberUIUserInfosLiveData(groupInfo.target, false).observe(this, uiUserInfos -> {
                if (uiUserInfos == null || uiUserInfos.isEmpty()) {
                    return;
                }
                List<String> memberIds = new ArrayList<>(uiUserInfos.size());
                for (UIUserInfo uiUserInfo : uiUserInfos) {
                    memberIds.add(uiUserInfo.getUserInfo().uid);
                }
                pickUserViewModel.setUncheckableIds(memberIds);
                userListAdapter.notifyDataSetChanged();
            });
        } else {
            PickUserViewModel pickUserViewModel = new ViewModelProvider(getActivity()).get(PickUserViewModel.class);
            List<UIUserInfo> uiUserInfos = UIUserInfo.fromUserInfos(userInfoList);

            List<String> memberIds = new ArrayList<>(uiUserInfos.size());
            for (UIUserInfo uiUserInfo : uiUserInfos) {
                memberIds.add(uiUserInfo.getUserInfo().uid);
            }
            pickUserViewModel.setUncheckableIds(memberIds);
            userListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void initHeaderViewHolders() {
        // do nothing
    }
}
