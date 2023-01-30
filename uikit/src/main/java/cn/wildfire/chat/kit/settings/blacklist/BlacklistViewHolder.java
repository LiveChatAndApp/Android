/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings.blacklist;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class BlacklistViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.userNameTextView)
    TextView userNameTextView;

    public BlacklistViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(String userId) {
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
        userNameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo));
        GlideUtil.load(itemView.getContext(), userInfo.portrait).into(portraitImageView);
    }
}
