/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search;

import android.view.View;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.newfriend.UserSearchModule;
import cn.wildfire.chat.kit.search.module.ChannelSearchModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.ConversationSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;

public class SearchPortalActivity extends SearchActivity {

    @Override
    protected void afterViews() {
        super.afterViews();
        editText.setHint("请输入搜索关键字");
        icon.setVisibility(View.GONE);
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {

        SearchableModule module = new UserSearchModule();
        modules.add(module);
        module = new ContactSearchModule();
        modules.add(module);
        module = new GroupSearchViewModule();
        modules.add(module);
        module = new ConversationSearchModule();
        modules.add(module);
        modules.add(new ChannelSearchModule());
    }
}
