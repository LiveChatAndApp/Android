/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupPageInfo;

public class GroupListAdapter extends PagedListAdapter<GroupPageInfo.Item, GroupViewHolder> {
    private Fragment fragment;
    private OnGroupItemClickListener onGroupItemClickListener;

    static DiffUtil.ItemCallback<GroupPageInfo.Item> showDiffCallBack = new DiffUtil.ItemCallback<GroupPageInfo.Item>() {

        @Override
        public boolean areItemsTheSame(@NonNull GroupPageInfo.Item oldItem, @NonNull GroupPageInfo.Item newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull GroupPageInfo.Item oldItem, @NonNull GroupPageInfo.Item newItem) {
            return oldItem.equals(newItem);
        }
    };

    public GroupListAdapter(Fragment fragment) {
        super(showDiffCallBack);
        this.fragment = fragment;
    }

//    public void setGroupInfos(List<GroupInfo> groupInfos) {
//        if (groupInfos == null) {
//            this.groupInfos.clear();
//            notifyDataSetChanged();
//            return;
//        }
//        this.groupInfos.clear();
//        this.groupInfos.addAll(groupInfos);
//        notifyDataSetChanged();
//    }
//
//    public List<GroupInfo> getGroupInfos() {
//        return groupInfos;
//    }

    public void setOnGroupItemClickListener(OnGroupItemClickListener onGroupItemClickListener) {
        this.onGroupItemClickListener = onGroupItemClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item_contact, parent, false);
        GroupViewHolder viewHolder = new GroupViewHolder(fragment, this, view);
        view.findViewById(R.id.contactLinearLayout).setOnClickListener(v -> {
            if (onGroupItemClickListener != null) {
                onGroupItemClickListener.onGroupClick(viewHolder.getGroupInfoItem());
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.onBind(getItem(position));
    }
}
