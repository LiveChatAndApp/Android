/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.AppServerFriendRequest;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
    private FriendRequestListFragment fragment;
    private FriendRequestListAdapter adapter;
    private AppServerFriendRequest friendRequest;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;

    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.introTextView)
    TextView introTextView;
    @BindView(R2.id.acceptButton)
    TextView acceptButton;
    @BindView(R2.id.rejectButton)
    TextView rejectButton;

    private FriendRequestListAdapter.OnItemClick onItemClickListener;

    public FriendRequestViewHolder(FriendRequestListFragment fragment, FriendRequestListAdapter adapter, View itemView, FriendRequestListAdapter.OnItemClick onItemClickListener) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
        userViewModel = new ViewModelProvider(fragment).get(UserViewModel.class);
        contactViewModel = new ViewModelProvider(fragment).get(ContactViewModel.class);
        this.onItemClickListener = onItemClickListener;
    }

    @OnClick(R2.id.acceptButton)
    void accept() {
        onItemClickListener.onItemClick(friendRequest, 1);
    }

    @OnClick(R2.id.rejectButton)
    void rejectButton() {
        onItemClickListener.onItemClick(friendRequest, 0);
    }

    public void onBind(AppServerFriendRequest friendRequest) {
        this.friendRequest = friendRequest;
        UserInfo userInfo = userViewModel.getUserInfo(friendRequest.uid, true);

        nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo.uid));
        acceptButton.setVisibility(View.VISIBLE);
        rejectButton.setVisibility(View.VISIBLE);
        String desc = TextUtils.isEmpty(friendRequest.helloText) ? fragment.getString(R.string.request_friend) : friendRequest.helloText;
        introTextView.setText(desc);

        if (userInfo != null) {
            GlideUtil.load(fragment, ChatManager.Instance().getUserPortrait(userInfo))
                    .apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop())
                    .into(portraitImageView);
        }
    }
}
