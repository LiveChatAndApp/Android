/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.UserInfo;

public class UserViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected UserListAdapter adapter;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;
    @BindView(R2.id.categoryTextView)
    protected TextView categoryTextView;

    protected UIUserInfo userInfo;

    public UserViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
        // 通讯录 setting
        setLabelColor(fragment.getResources().getColor(R.color.textBlack), fragment.getResources().getColor(R.color.white));
    }

    /**
     * 设定名称上方的label 颜色
     */
    protected void setLabelColor(int textColor, int bgColor) {
        categoryTextView.setTextColor(textColor);
        categoryTextView.setBackgroundColor(bgColor);
    }

    public void onBind(UIUserInfo userInfo) {
        this.userInfo = userInfo;
        if (userInfo.isShowCategory()) {
            categoryTextView.setVisibility(View.VISIBLE);
            categoryTextView.setText(userInfo.getCategory());
        } else {
            categoryTextView.setVisibility(View.GONE);
        }
        UserViewModel userViewModel = new ViewModelProvider(fragment).get(UserViewModel.class);
        UserInfo info = userInfo.getUserInfo();
//        if (TextUtils.isEmpty(userInfo.getUserInfo().portrait) || !userInfo.getUserInfo().portrait.contains("http")) {
//            info = userViewModel.getUserInfo(userInfo.getUserInfo().uid, true);
//            userInfo.setUserInfo(info);
//        }
        nameTextView.setText(userViewModel.getUserDisplayName(info));
        if (!TextUtils.isEmpty(userInfo.getDesc())) {
            descTextView.setVisibility(View.VISIBLE);
            descTextView.setText(userInfo.getDesc());
        } else {
            descTextView.setVisibility(View.GONE);
        }
        GlideUtil.load(fragment, info.portrait)
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(10))
                .into(portraitImageView);
    }

    public UIUserInfo getBindContact() {
        return userInfo;
    }
}
