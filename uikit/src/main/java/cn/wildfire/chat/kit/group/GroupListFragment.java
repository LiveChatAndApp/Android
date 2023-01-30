/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.group.page.GroupDataSourceFactory;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupPageInfo;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupsCallback;

/**
 * 群聊列表
 */
public class GroupListFragment extends Fragment implements OnGroupItemClickListener {
    String TAG = getClass().getSimpleName();
    @BindView(R2.id.groupRecyclerView)
    RecyclerView recyclerView;
    @BindView(R2.id.tipTextView)
    TextView tipTextView;
    @BindView(R2.id.groupsLinearLayout)
    LinearLayout groupsLinearLayout;
    @BindView(R2.id.refreshSwipeRefreshLayout)
    SwipeRefreshLayout refreshSwipeRefreshLayout;

    private GroupListAdapter groupListAdapter;
    private OnGroupItemClickListener onGroupItemClickListener;
    private GroupViewModel viewModel;

    public void setOnGroupItemClickListener(OnGroupItemClickListener onGroupItemClickListener) {
        this.onGroupItemClickListener = onGroupItemClickListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_list_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.resetMyGroupList();
    }

    private void observerMyGroupList() {
        viewModel.observerMyGroupList(this).observe(getViewLifecycleOwner(), result -> {
            if (getActivity() == null) {
                return;
            }
            try {
                LogHelper.e("GroupPageDataSource", "observerMyGroupList 更新 " + result.size());
                groupListAdapter.submitList(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void init() {
        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        viewModel.resetMyGroupList();
        groupListAdapter = new GroupListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(groupListAdapter);
        groupListAdapter.setOnGroupItemClickListener(this);

        refreshSwipeRefreshLayout.setOnRefreshListener(() -> {
            refreshSwipeRefreshLayout.setRefreshing(false);
            viewModel.resetMyGroupList();
        });
        observerMyGroupList();
    }

    @Override
    public void onGroupClick(GroupPageInfo.Item groupInfo) {
        if (groupInfo == null) {
            return;
        }
        if (onGroupItemClickListener != null) {
            onGroupItemClickListener.onGroupClick(groupInfo);
            return;
        }
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfo.gid);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
    }
}
