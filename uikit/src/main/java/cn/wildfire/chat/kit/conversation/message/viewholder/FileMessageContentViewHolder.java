/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ComponentName;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(FileMessageContent.class)
@EnableContextMenu
public class FileMessageContentViewHolder extends MediaMessageContentViewHolder {
    @BindView(R2.id.fileIconImageView)
    ImageView fileIconImageView;
    @BindView(R2.id.fileNameTextView)
    TextView nameTextView;
    @BindView(R2.id.fileSizeTextView)
    TextView sizeTextView;

    private FileMessageContent fileMessageContent;

    public FileMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        fileMessageContent = (FileMessageContent) message.message.content;
        nameTextView.setText(fileMessageContent.getName());
        sizeTextView.setText(FileUtils.getReadableFileSize(fileMessageContent.getSize()));
        fileIconImageView.setImageResource(FileUtils.getFileTypeImageResId(fileMessageContent.getName()));
    }

    @OnClick(R2.id.fileMessageContentItemView)
    public void onClick(View view) {
        if (message.isDownloading) {
            return;
        }
        File file = DownloadManager.mediaMessageContentFile(message.message);
        if (file == null) {
            return;
        }
        ChatManager.Instance().setMediaMessagePlayed(message.message.messageId);

        if (file.exists()) {
            Intent intent = FileUtils.getViewIntent(fragment.getContext(), file);
            ComponentName cn = intent.resolveActivity(fragment.getContext().getPackageManager());
            if (cn == null) {
                Toast.makeText(fragment.getContext(), "找不到能打开此文件的应用", Toast.LENGTH_SHORT).show();
                return;
            }
            fragment.startActivity(intent);
        } else {
            String fileUrl;
            if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
                fileUrl = DownloadManager.buildSecretChatMediaUrl(message.message);
            } else {
                fileUrl = ((FileMessageContent) message.message.content).remoteUrl;
            }
            DownloadManager.download(fileUrl, file.getParent(), file.getName(), new DownloadManager.OnDownloadListener() {
                @Override
                public void onSuccess(File file) {
                    if (fragment.getActivity() != null && !fragment.getActivity().isFinishing()) {
                        Intent intent = FileUtils.getViewIntent(fragment.getContext(), file);
                        ComponentName cn = intent.resolveActivity(fragment.getContext().getPackageManager());
                        if (cn == null) {
                            Toast.makeText(fragment.getContext(), "找不到能打开此文件的应用", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        fragment.startActivity(intent);
                    }
                }

                @Override
                public void onProgress(int progress) {

                }

                @Override
                public void onFail() {

                }
            });
        }
    }
}
