package cn.wildfire.chat.kit.group.page;

import androidx.paging.DataSource;

import cn.wildfirechat.model.GroupPageInfo;

public class GroupDataSourceFactory extends DataSource.Factory<Integer, GroupPageInfo.Item> {

    @Override
    public DataSource<Integer,GroupPageInfo.Item> create() {
        return new GroupPageDataSource();
    }
}