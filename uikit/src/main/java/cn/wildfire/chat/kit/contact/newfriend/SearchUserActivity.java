/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.view.View;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.search.SearchActivity;
import cn.wildfire.chat.kit.search.SearchableModule;

/**
 *  搜寻用户 添加好友
 */
public class SearchUserActivity extends SearchActivity {

    @Override
    protected void afterViews() {
        super.afterViews();
        editText.setHint("搜索用户账号");
        icon.setVisibility(View.GONE);
    }

    @Override
    protected void beforeViews() {
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new UserSearchModule());
    }
}
