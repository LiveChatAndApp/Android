/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat;

import java.util.List;

import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.SearchUserCallback;
import cn.wildfirechat.remote.UserInfoCallback;

public interface UserSource {
    UserInfo getUser(String userId, UserInfoCallback<UserInfo> callback);
    //List<UserInfo> getUsers(List<String> userIds);

    void searchUser(String keyword, final SearchUserCallback callback);

    void modifyMyInfo(List<ModifyMyInfoEntry> values, UserInfoCallback<UserInfo> callback);
}
