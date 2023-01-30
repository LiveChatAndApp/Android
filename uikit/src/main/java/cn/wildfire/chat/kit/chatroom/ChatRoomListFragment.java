/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.chatroom;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.CustomChatRoomInfo;

public class ChatRoomListFragment extends Fragment implements OnClickChatroomItemListener {

    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;

    private LinearLayoutManager layoutManager;
    private ChatRoomAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatroom_list_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    private void init() {
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatRoomAdapter();
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        getChatRoomList();
    }

    private void getChatRoomList() {
        ChatRoomViewModel viewModel = new ViewModelProvider(this).get(ChatRoomViewModel.class);
        viewModel.getChatRoomList().observe(getViewLifecycleOwner(), response -> {
            if (!response.isSuccess()) {
                Toast.makeText(getContext(), response.message, Toast.LENGTH_SHORT).show();
                return;
            }
            adapter.setData(response.result);
            int start = layoutManager.findFirstVisibleItemPosition();
            int end = layoutManager.findLastVisibleItemPosition();
            adapter.notifyItemRangeChanged(start, end - start + 1, response.result);
        });
    }

    @Override
    public void onClickChatroomItem(CustomChatRoomInfo chatRoomInfo) {
        //todo 这里应该是先进入到ConversationActivity界面，然后在界面内joinchatroom？
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, chatRoomInfo.cid);
        intent.putExtra("conversation", conversation);
        intent.putExtra("conversationTitle", chatRoomInfo.name);
        startActivity(intent);
    }
}
