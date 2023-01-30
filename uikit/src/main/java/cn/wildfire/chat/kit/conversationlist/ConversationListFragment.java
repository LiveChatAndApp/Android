/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.HomeBaseFragment;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.ConversationViewModel;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationViewModel;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.PersonalDetailActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupPageInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 聊天室列表fragment
 */
public class ConversationListFragment extends ProgressFragment {
    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    public static final List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
            Conversation.ConversationType.Group,
            Conversation.ConversationType.Channel,
            Conversation.ConversationType.SecretChat);
    public static final List<Integer> lines = Arrays.asList(0);

    private ConversationListViewModel conversationListViewModel;
    private SettingViewModel settingViewModel;
    private LinearLayoutManager layoutManager;
    private OnClickConversationItemListener onClickConversationItemListener;
    private GroupViewModel groupViewModel;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    protected int contentLayout() {
        setHasOptionsMenu(true);
        return R.layout.conversationlist_frament;
    }

    @Override
    protected void afterViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        init();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (adapter != null && isVisibleToUser) {
            reloadConversations();
        }
    }

    public void setOnClickConversationItemListener(OnClickConversationItemListener listener) {
        this.onClickConversationItemListener = listener;
        if (adapter != null) {
            adapter.setOnClickConversationItemListener(listener);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadConversations();
    }

    private void init() {
        if (conversationListViewModel == null) {
            conversationListViewModel = new ViewModelProvider(this, new ConversationListViewModelFactory(types, lines))
                    .get(ConversationListViewModel.class);
        }
        if (groupViewModel == null) {
            groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        }

        adapter = new ConversationListAdapter(this);
        if (onClickConversationItemListener != null) {
            adapter.setOnClickConversationItemListener(onClickConversationItemListener);
        }
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        getConversation();

        StatusNotificationViewModel statusNotificationViewModel = WfcUIKit.getAppScopeViewModel(StatusNotificationViewModel.class);
        statusNotificationViewModel.statusNotificationLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                LogHelper.e("ImConnectStatus", "" + o);
                adapter.updateStatusNotification(statusNotificationViewModel.getNotificationItems());
            }
        });
        conversationListViewModel.connectionStatusLiveData().observe(this, status -> {
            ConnectionStatusNotification connectionStatusNotification = new ConnectionStatusNotification();
            switch (status) {
                case ConnectionStatus.ConnectionStatusConnecting:
                    LogHelper.e("ImConnectStatus", "正在连接");
                    connectionStatusNotification.setValue("正在连接...");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusReceiveing:
                    LogHelper.e("ImConnectStatus", "正在同步");
                    connectionStatusNotification.setValue("正在同步...");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusConnected:
                    LogHelper.e("ImConnectStatus", "已连接");
                    statusNotificationViewModel.hideStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusUnconnected:
                    LogHelper.e("ImConnectStatus", "连接失败");
                    connectionStatusNotification.setValue("连接失败");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                default:
                    break;
            }
        });
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        settingViewModel.settingUpdatedLiveData().observe(this, o -> {
            if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
                return;
            }
            conversationListViewModel.reloadConversationList(true);
            conversationListViewModel.reloadConversationUnreadStatus();

            List<PCOnlineInfo> infos = ChatManager.Instance().getPCOnlineInfos();
            statusNotificationViewModel.clearStatusNotificationByType(PCOnlineStatusNotification.class);
            if (infos.size() > 0) {
                for (PCOnlineInfo info : infos) {
                    PCOnlineStatusNotification notification = new PCOnlineStatusNotification(info);
                    statusNotificationViewModel.showStatusNotification(notification);

                    SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
                    sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
                }
            }
        });
        List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
        if (pcOnlineInfos != null && !pcOnlineInfos.isEmpty()) {
            for (PCOnlineInfo info : pcOnlineInfos) {
                PCOnlineStatusNotification notification = new PCOnlineStatusNotification(info);
                statusNotificationViewModel.showStatusNotification(notification);

                SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
                sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
            }
        }
        syncGroupList();
        showFirstRegisterDialog();
    }

    private void getConversation() {
        if (conversationListViewModel == null) {
            conversationListViewModel = new ViewModelProvider(this, new ConversationListViewModelFactory(types, lines))
                    .get(ConversationListViewModel.class);
        }
        // 讀取聊天列表
        conversationListViewModel.conversationListLiveData().observe(this, conversationInfos -> {
            showContent();
            adapter.setConversationInfos(conversationInfos);
            getUserData();
            getGroupData();
        });
    }

    /**
     * 监听 取得 group 更新资料
     */
    private void getGroupData() {
        if (groupViewModel == null) {
            groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        }
        // 群聊的聊天室资料会跟著conversationListLiveData一起回来，这边是取得 标题 头像等详细资料
        groupViewModel.groupInfoUpdateLiveData().observe(this, new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });
    }

    /**
     * 取广播群
     */
    private void syncGroupList() {
        if (groupViewModel == null) {
            groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        }
        groupViewModel.getGroupList(2, 0, 20).observe(getViewLifecycleOwner(), response -> {
            if (!response.isSuccess()) {
                Toast.makeText(getContext(), response.message, Toast.LENGTH_SHORT).show();
                return;
            }
            if (response.result != null) {
                // 更新廣播 conversation
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.updateConversationInfos(response.result);
                adapter.notifyItemRangeChanged(start, end - start + 1);
                reloadConversations();
            }
        });
    }

    /**
     * 监听 个人资料更新
     */
    private void getUserData() {
        if (userViewModel == null) {
            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        }
        userViewModel.userInfoLiveData().observe(this, new Observer<List<UserInfo>>() {
            @Override
            public void onChanged(List<UserInfo> userInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });
    }

    private void reloadConversations() {
        if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
            return;
        }
        // 取聊天列表
        conversationListViewModel.reloadConversationList();
        // 未读讯息数量
        conversationListViewModel.reloadConversationUnreadStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private boolean showFirstRegisterDialog() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
        boolean isFirstRegisterLogin = sharedPreferences.getBoolean("isFirstRegisterLogin", false);
        if (isFirstRegisterLogin) {
            new MaterialDialog.Builder(getContext())
                    .title(cn.wildfire.chat.kit.R.string.first_login_title)
                    .content(cn.wildfire.chat.kit.R.string.first_login_content)
                    .positiveColorRes(R.color.blueBtn)
                    .positiveText(cn.wildfire.chat.kit.R.string.first_login_go_btn)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            sharedPreferences.edit()
                                    .putBoolean("isFirstRegisterLogin", false)
                                    .apply();
                            String uid = userViewModel.getUserId();
                            UserInfo userInfo = userViewModel.getUserInfo(uid,null,false);
                            Intent intent = new Intent(getActivity(), PersonalDetailActivity.class);
                            intent.putExtra("userInfo", userInfo);
                            startActivity(intent);

                            dialog.dismiss();
                        }
                    })
                    .negativeText(cn.wildfire.chat.kit.R.string.first_login_no_btn)
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            sharedPreferences.edit()
                                    .putBoolean("isFirstRegisterLogin", false)
                                    .apply();
                            reloadConversations();
                            dialog.dismiss();
                        }
                    })
                    .cancelable(false)
                    .show();
        }
        return isFirstRegisterLogin;
    }
}
