/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.GroupPageInfo;

public class GroupViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    private GroupListAdapter adapter;
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.memberCountTextView)
    TextView memberCountTextView;
    @BindView(R2.id.categoryTextView)
    TextView categoryTextView;
    @BindView(R2.id.dividerLine)
    View dividerLine;

    protected GroupPageInfo.Item item;

    public GroupViewHolder(Fragment fragment, GroupListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
    }

    // TODO hide the last diver line
    public void onBind(GroupPageInfo.Item item) {
        if (item == null) {
            return;
        }
        this.item = item;
        String portrait;
        if (!TextUtils.isEmpty(item.portrait)) {
            portrait = item.portrait;
        } else {
            GroupInfo groupInfo = ChatManagerHolder.gChatManager.getGroupInfo(item.gid, false);
            if (groupInfo != null) {
                portrait = groupInfo.portrait;
            } else {
                portrait = null;
            }

            if (TextUtils.isEmpty(portrait)) {
                portrait = ImageUtils.getGroupGridPortrait(fragment.getContext(), item.gid, 60);
            }
        }

        GlideUtil.load(fragment, portrait).
                placeholder(R.mipmap.ic_group_default_portrait).
                into(portraitImageView);

        String name = item.groupName;
        String count = "";
        // 一般群组要显示 人数
        List<GroupMember> list = ChatManagerHolder.gChatManager.getGroupMembers(item.gid, false);
        if (list != null && list.size() > 0) {
            count = "(" + list.size() + ")";
        }

        categoryTextView.setVisibility(View.GONE);
        nameTextView.setText(name);
        memberCountTextView.setText(count);
    }

    public GroupPageInfo.Item getGroupInfoItem() {
        return item;
    }
}
