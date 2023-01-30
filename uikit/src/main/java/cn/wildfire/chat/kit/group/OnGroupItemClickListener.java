/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupPageInfo;

public interface OnGroupItemClickListener {
    void onGroupClick(GroupPageInfo.Item groupInfo);
}
