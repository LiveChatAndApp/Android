package cn.wildfire.chat.kit.group.page;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.GroupPageInfo;
import cn.wildfirechat.model.WebResponse;

/**
 * GroupListFragment 分页 page
 */
public class GroupPageDataSource extends PositionalDataSource<GroupPageInfo.Item> {
    int page = 0;

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<GroupPageInfo.Item> callback) {
        int requestedLoadSize = params.requestedLoadSize;
        LogHelper.d("GroupPageDataSource", "PositionPageDataSource loadInitial " + requestedLoadSize);

        ImplementUserSource.Instance().getGroupList(1, params.requestedStartPosition, requestedLoadSize, new SimpleCallback<WebResponse<GroupPageInfo>>() {
            @Override
            public void onUiSuccess(WebResponse<GroupPageInfo> response) {
                if (response.result == null) {
                    return;
                }
                List<GroupPageInfo.Item> list = response.result.data;
                callback.onResult(list, params.requestedStartPosition, response.getResult().totalElement);//初始化加载从index == 0开始 40假设为数据总数
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<GroupPageInfo.Item> callback) {
        LogHelper.d("GroupPageDataSource", "PositionPageDataSource loadRange");
        LogHelper.d("GroupPageDataSource", "params.requestedStartPosition" + params.startPosition);
        page = params.startPosition / params.loadSize;
        ImplementUserSource.Instance().getGroupList(1, page, params.loadSize, new SimpleCallback<WebResponse<GroupPageInfo>>() {
            @Override
            public void onUiSuccess(WebResponse<GroupPageInfo> response) {
                List<GroupPageInfo.Item> list = response.result.data;
                callback.onResult(list);
            }

            @Override
            public void onUiFailure(int code, String msg) {

            }
        });
    }
}
