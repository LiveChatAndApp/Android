package cn.wildfire.chat.kit.chatroom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfirechat.model.CustomChatRoomInfo;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatViewHolder> {

    private List<CustomChatRoomInfo> list = new ArrayList<>();
    private OnClickChatroomItemListener listener;
    private RequestOptions mOptions;

    public ChatRoomAdapter() {
        mOptions = new RequestOptions()
                .placeholder(R.mipmap.ic_group_default_portrait)
                .transforms(new CenterCrop(), new RoundedCorners(4));
    }

    public void setListener(OnClickChatroomItemListener listener) {
        this.listener = listener;
    }

    public void setData(List<CustomChatRoomInfo> list) {
        if (list == null) {
            return;
        }
        this.list.clear();
        this.list.addAll(list);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ChatViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.text)
        TextView text;
        @BindView(R2.id.icon)
        ImageView icon;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener((view) -> {
                if (listener == null) {
                    return;
                }
                int position = getAdapterPosition();
                listener.onClickChatroomItem(list.get(position));
            });
        }

        public void bind(CustomChatRoomInfo info) {
            GlideUtil.load(icon, info.image).apply(mOptions).into(icon);
            text.setText(info.name);
        }
    }
}
