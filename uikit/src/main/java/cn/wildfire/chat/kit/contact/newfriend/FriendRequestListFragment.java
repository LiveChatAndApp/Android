/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.AppServerFriendRequest;
import cn.wildfirechat.model.FriendRequest;

/**
 * 好友请求
 */
public class FriendRequestListFragment extends Fragment implements FriendRequestListAdapter.OnItemClick {
    @BindView(R2.id.noNewFriendLinearLayout)
    LinearLayout noNewFriendLinearLayout;
    @BindView(R2.id.newFriendListLinearLayout)
    LinearLayout newFriendLinearLayout;
    @BindView(R2.id.friendRequestListRecyclerView)
    RecyclerView recyclerView;

    private ContactViewModel contactViewModel;
    private FriendRequestListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_new_friend_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), userInfos -> {
            if (adapter != null) {
                adapter.onUserInfosUpdate(userInfos);
            }
        });
        getFriendRequest();
//        List<FriendRequest> requests = contactViewModel.getFriendRequest();
//        if (requests != null && requests.size() > 0) {
//            noNewFriendLinearLayout.setVisibility(View.GONE);
//            newFriendLinearLayout.setVisibility(View.VISIBLE);
//
//            adapter = new FriendRequestListAdapter(FriendRequestListFragment.this);
//            adapter.setFriendRequests(requests);
//            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//            recyclerView.setAdapter(adapter);
//        } else {
//            noNewFriendLinearLayout.setVisibility(View.VISIBLE);
//            newFriendLinearLayout.setVisibility(View.GONE);
//        }
        contactViewModel.clearUnreadFriendRequestStatus();
    }

    private void getFriendRequest() {
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .content("刷新请求资料")
                .progress(true, 100)
                .build();
        dialog.show();
        contactViewModel.getFriendRequestAppService().observe(getViewLifecycleOwner(), result -> {
            dialog.dismiss();
            if (result != null && result.size() > 0) {
                noNewFriendLinearLayout.setVisibility(View.GONE);
                newFriendLinearLayout.setVisibility(View.VISIBLE);

                adapter = new FriendRequestListAdapter(FriendRequestListFragment.this, this);
                adapter.setFriendRequests(result);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                recyclerView.setAdapter(adapter);
            } else {
                noNewFriendLinearLayout.setVisibility(View.VISIBLE);
                newFriendLinearLayout.setVisibility(View.GONE);
            }
            contactViewModel.clearUnreadFriendRequestStatus();
        });
    }

    @Override
    public void onItemClick(AppServerFriendRequest request, int reply) {
        if (reply == 1 && request.verify) {
            showVerifyDialog(request.uid);
        } else {
            send(request.uid, reply, "");
        }
    }

    private void send(String uid, int reply, String verify) {
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .content("回复通知中...")
                .progress(true, 100)
                .build();
        dialog.show();

        contactViewModel.responseFriendRequest(uid, reply, verify).observe(this, response -> {
            dialog.dismiss();
            if (response.isSuccess()) {
                int msg = reply == 1 ? R.string.verify_success_add_friend : R.string.verify_success_reject_friend;
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(this::getFriendRequest, 500);
            } else if (response.code == 1038) {
                Toast.makeText(getActivity(), R.string.verify_error_add_friend, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), response.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 输入好友验证 dialog
    private void showVerifyDialog(String uid) {
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .customView(R.layout.dialog_friend_verify, false)
                .build();
        EditText editText = dialog.getCustomView().findViewById(R.id.editText);
        TextView cancel = dialog.getCustomView().findViewById(R.id.cancel);
        TextView submit = dialog.getCustomView().findViewById(R.id.submit);
        cancel.setOnClickListener((view) -> {
            dialog.dismiss();
        });
        submit.setOnClickListener((view) -> {
            try {
                String verify = editText.getText().toString();
                if (TextUtils.isEmpty(verify)) {
                    Toast.makeText(getActivity(), getString(R.string.friend_verify_not_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                send(uid, 1, verify);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        });
        dialog.getWindow().getDecorView().setBackground(null);
        dialog.show();
    }
}
