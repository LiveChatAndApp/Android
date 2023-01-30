/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GetPhotoFragment;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.file.FileRecordActivity;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfire.chat.kit.group.AddGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupAnnouncement;
import cn.wildfire.chat.kit.group.GroupMemberListActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.RemoveGroupMemberActivity;
import cn.wildfire.chat.kit.group.SetGroupAnnouncementActivity;
import cn.wildfire.chat.kit.group.SetGroupNameActivity;
import cn.wildfire.chat.kit.group.SetGroupRemarkActivity;
import cn.wildfire.chat.kit.group.manage.GroupManageActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.search.SearchMessageActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.PersonalDetailActivity;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.ModifyGroupInfoType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

public class GroupConversationInfoFragment extends GetPhotoFragment implements ConversationMemberAdapter.OnMemberClickListener, CompoundButton.OnCheckedChangeListener {

    @BindView(R2.id.progressBar)
    ProgressBar progressBar;

    @BindView(R2.id.contentNestedScrollView)
    NestedScrollView contentNestedScrollView;

    // group
    @BindView(R2.id.groupLinearLayout_0)
    LinearLayout groupLinearLayout_0;
    @BindView(R2.id.groupNameOptionItemView)
    OptionItemView groupNameOptionItemView;
    @BindView(R2.id.groupRemarkOptionItemView)
    OptionItemView groupRemarkOptionItemView;
    @BindView(R2.id.groupQRCodeOptionItemView)
    OptionItemView groupQRCodeOptionItemView;
    @BindView(R2.id.groupNoticeLinearLayout)
    LinearLayout noticeLinearLayout;
    @BindView(R2.id.groupNoticeTextView)
    TextView noticeTextView;
    @BindView(R2.id.groupManageOptionItemView)
    OptionItemView groupManageOptionItemView;
    @BindView(R2.id.groupManageDividerLine)
    View groupManageDividerLine;
    @BindView(R2.id.showAllMemberButton)
    Button showAllGroupMemberButton;

    @BindView(R2.id.groupLinearLayout_1)
    LinearLayout groupLinearLayout_1;
    @BindView(R2.id.myGroupNickNameOptionItemView)
    OptionItemView myGroupNickNameOptionItemView;
    @BindView(R2.id.showGroupMemberAliasSwitchButton)
    SwitchMaterial showGroupMemberNickNameSwitchButton;

    @BindView(R2.id.quitButton)
    TextView quitGroupButton;

    @BindView(R2.id.markGroupLinearLayout)
    LinearLayout markGroupLinearLayout;
    @BindView(R2.id.markGroupSwitchButton)
    SwitchMaterial markGroupSwitchButton;

    // common
    @BindView(R2.id.memberRecyclerView)
    RecyclerView memberReclerView;
    @BindView(R2.id.groupTopLinearLayout)
    LinearLayout groupTopLinearLayout;
    @BindView(R2.id.stickTopSwitchButton)
    SwitchMaterial stickTopSwitchButton;
    @BindView(R2.id.silentSwitchButton)
    SwitchMaterial silentSwitchButton;

    @BindView(R2.id.fileRecordOptionItemView)
    OptionItemView fileRecordOptionItem;

    @BindView(R2.id.portraitImageView)
    ShapeableImageView portraitImageView;

    @BindView(R2.id.clearMessagesOptionItemView)
    TextView clearMessagesOptionItemView;
    @BindView(R2.id.groupTopTextView)
    TextView groupTopTextView;

    @BindView(R2.id.arrowImageView)
    ImageView arrowImageView;

    private ConversationInfo conversationInfo;
    private ConversationMemberAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    private GroupViewModel groupViewModel;
    private GroupInfo groupInfo;
    // me in group
    private GroupMember groupMember;


    public static GroupConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        GroupConversationInfoFragment fragment = new GroupConversationInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("conversationInfo", conversationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        assert args != null;
        conversationInfo = args.getParcelable("conversationInfo");
        assert conversationInfo != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversation_info_group_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.group_conversation_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.qr_code) {
            Intent intent = QRCodeActivity.buildQRCodeIntent(getActivity(), "群二维码", QRCodeActivity.TYPE_GROUP, groupInfo.target);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndShowGroupNotice();
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        groupLinearLayout_0.setVisibility(View.VISIBLE);
        groupLinearLayout_1.setVisibility(View.VISIBLE);
        markGroupLinearLayout.setVisibility(View.VISIBLE);
        markGroupSwitchButton.setOnCheckedChangeListener(this);
        quitGroupButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupInfo = groupViewModel.getGroupInfo(conversationInfo.conversation.target, true);
        if (groupInfo != null) {
            groupMember = ChatManager.Instance().getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        } else {
            getActivity().finish();
            return;
        }

        if (groupMember == null || groupMember.type == GroupMember.GroupMemberType.Removed) {
            Toast.makeText(getActivity(), "你不在群组或发生错误, 请稍后再试", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
        loadAndShowGroupMembers(true);

        userViewModel.userInfoLiveData().observe(this, userInfos -> loadAndShowGroupMembers(false));
        observerFavGroupsUpdate();
        observerGroupInfoUpdate();
        observerGroupMembersUpdate();

        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }
        initBroadcastGroup();
    }

    /**
     * 初始化 广播群 画面
     */
    private void initBroadcastGroup() {
        if (!groupInfo.isBroadCastGroup()) {
            return;
        }
        showAllGroupMemberButton.setVisibility(View.GONE);
        memberReclerView.setVisibility(View.GONE);
        clearMessagesOptionItemView.setVisibility(View.GONE);
        quitGroupButton.setVisibility(View.GONE);
        groupRemarkOptionItemView.setVisibility(View.GONE);
        markGroupLinearLayout.setVisibility(View.GONE);
        myGroupNickNameOptionItemView.setVisibility(View.GONE);
        stickTopSwitchButton.setEnabled(false);
        stickTopSwitchButton.setChecked(true);
        groupTopTextView.setTextColor(getResources().getColor(R.color.gray23));
    }

    private void observerFavGroupsUpdate() {
        groupViewModel.getFavGroups().observe(getViewLifecycleOwner(), listOperateResult -> {
            if (listOperateResult.isSuccess()) {
                for (GroupInfo info : listOperateResult.getResult()) {
                    if (info != null && groupInfo.target.equals(info.target)) {
                        markGroupSwitchButton.setChecked(true);
                        break;
                    }
                }
            }
        });
    }

    private void observerGroupMembersUpdate() {
        groupViewModel.groupMembersUpdateLiveData().observe(this, groupMembers -> {
            loadAndShowGroupMembers(false);
        });
    }

    private void observerGroupInfoUpdate() {
        groupViewModel.groupInfoUpdateLiveData().observe(this, groupInfos -> {
            for (GroupInfo groupInfo : groupInfos) {
                if (groupInfo.target.equals(this.groupInfo.target)) {
                    this.groupInfo = groupInfo;
                    groupNameOptionItemView.setDesc(groupInfo.name);
                    groupRemarkOptionItemView.setDesc(groupInfo.remark);
                    loadAndShowGroupMembers(false);
                    break;
                }
            }

        });
    }

    private void showPortrait(GroupInfo info, List<UserInfo> members) {
        if (TextUtils.isEmpty(info.portrait)) {
            groupViewModel.getGroupImage(getContext(), members).observe(getViewLifecycleOwner(), portrait -> {
                if (portrait != null) {
                    GlideUtil.load(GroupConversationInfoFragment.this, portrait)
                            .placeholder(R.mipmap.ic_group_default_portrait)
                            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 4)))
                            .into(portraitImageView);
                }
            });
        } else {
            GlideUtil.load(GroupConversationInfoFragment.this, info.portrait)
                    .placeholder(R.mipmap.ic_group_default_portrait)
                    .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 4)))
                    .into(portraitImageView);
        }

        if (groupInfo.type != GroupInfo.GroupType.Restricted
                || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            arrowImageView.setVisibility(View.VISIBLE);
        } else {
            arrowImageView.setVisibility(View.GONE);
        }
    }

    private void loadAndShowGroupMembers(boolean refresh) {
        groupViewModel.getGroupMembersLiveData(conversationInfo.conversation.target, refresh)
                .observe(this, groupMembers -> {
                    progressBar.setVisibility(View.GONE);
                    showGroupMembers(groupMembers);
                    showGroupManageViews();
                    contentNestedScrollView.setVisibility(View.VISIBLE);
                });
    }

    private void loadAndShowGroupNotice() {

        WfcUIKit.getWfcUIKit().getAppServiceProvider().getGroupAnnouncement(groupInfo.target, new AppServiceProvider.GetGroupAnnouncementCallback() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (TextUtils.isEmpty(announcement.text)) {
                    noticeTextView.setVisibility(View.GONE);
                } else {
                    noticeTextView.setText(announcement.text);
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                noticeTextView.setVisibility(View.GONE);
            }
        });
    }

    private void showGroupManageViews() {
        if (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner) {
            groupManageOptionItemView.setVisibility(View.VISIBLE);
        }

        showGroupMemberNickNameSwitchButton.setChecked("1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target)));
        showGroupMemberNickNameSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userViewModel.setUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target, isChecked ? "1" : "0");
        });

        myGroupNickNameOptionItemView.setDesc(groupMember.alias);
        groupNameOptionItemView.setDesc(groupInfo.name);
        groupRemarkOptionItemView.setDesc(groupInfo.remark);

        // 广播群 不能设定置顶
        if(!groupInfo.isBroadCastGroup()){
            stickTopSwitchButton.setChecked(conversationInfo.top > 0);
            stickTopSwitchButton.setOnCheckedChangeListener(this);
        }
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        silentSwitchButton.setOnCheckedChangeListener(this);

        if (groupInfo != null && ChatManagerHolder.gChatManager.getUserId().equals(groupInfo.owner)) {
            quitGroupButton.setText(R.string.delete_and_dismiss);
        } else {
            quitGroupButton.setText(R.string.delete_and_exit);
        }
    }

    private void showGroupMembers(List<GroupMember> groupMembers) {
        if (groupMembers == null || groupMembers.isEmpty()) {
            return;
        }
        String userId = ChatManager.Instance().getUserId();
        List<String> memberIds = new ArrayList<>();
        for (GroupMember member : groupMembers) {
            memberIds.add(member.memberId);
        }

        boolean enableRemoveMember = false;
        boolean enableAddMember = false;
        if (groupInfo.joinType == 2) {
            if (groupMember.type == GroupMember.GroupMemberType.Owner || groupMember.type == GroupMember.GroupMemberType.Manager) {
                enableAddMember = true;
                enableRemoveMember = true;
            }
        } else {
            enableAddMember = true;
            if (groupMember.type != GroupMember.GroupMemberType.Normal || userId.equals(groupInfo.owner)) {
                enableRemoveMember = true;
            }
        }
        int maxShowMemberCount = 45;
        if (enableAddMember) {
            maxShowMemberCount--;
        }
        if (enableRemoveMember) {
            maxShowMemberCount--;
        }
        if (memberIds.size() > maxShowMemberCount) {
            if (!groupInfo.isBroadCastGroup()) {
                showAllGroupMemberButton.setVisibility(View.VISIBLE);
            }
            memberIds = memberIds.subList(0, maxShowMemberCount);
        }

        conversationMemberAdapter = new ConversationMemberAdapter(conversationInfo, enableAddMember, enableRemoveMember);
        List<UserInfo> members = UserViewModel.getUsers(memberIds, groupInfo.target);
        showPortrait(groupInfo, members);
        conversationMemberAdapter.setMembers(members);
        conversationMemberAdapter.setOnMemberClickListener(this);
        memberReclerView.setAdapter(conversationMemberAdapter);
        memberReclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        memberReclerView.setNestedScrollingEnabled(false);
        memberReclerView.setHasFixedSize(true);
        memberReclerView.setFocusable(false);
    }

    @OnClick(R2.id.groupNameOptionItemView)
    void updateGroupName() {
        if (groupInfo.type != GroupInfo.GroupType.Restricted
                || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupNameActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.no_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R2.id.groupRemarkOptionItemView)
    void updateGroupRemark() {
        Intent intent = new Intent(getActivity(), SetGroupRemarkActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.groupNoticeLinearLayout)
    void updateGroupNotice() {
        if (groupInfo.type != GroupInfo.GroupType.Restricted
                || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            Intent intent = new Intent(getActivity(), SetGroupAnnouncementActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.no_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R2.id.groupManageOptionItemView)
    void manageGroup() {
        Intent intent = new Intent(getActivity(), GroupManageActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.showAllMemberButton)
    void showAllGroupMember() {
        Intent intent = new Intent(getActivity(), GroupMemberListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.myGroupNickNameOptionItemView)
    void updateMyGroupAlias() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .input("请输入你的群昵称", groupMember.alias, true, (dialog1, input) -> {
                    if (TextUtils.isEmpty(groupMember.alias)) {
                        if (TextUtils.isEmpty(input.toString().trim())) {
                            return;
                        }
                    } else if (groupMember.alias.equals(input.toString().trim())) {
                        return;
                    }

                    groupViewModel.modifyMyGroupAlias(groupInfo.target, input.toString().trim(), null, Collections.singletonList(0))
                            .observe(GroupConversationInfoFragment.this, operateResult -> {
                                if (operateResult.isSuccess()) {
                                    groupMember.alias = input.toString().trim();
                                    myGroupNickNameOptionItemView.setDesc(input.toString().trim());
                                } else {
                                    Toast.makeText(getActivity(), "修改群昵称失败:" + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .negativeText("取消")
                .positiveText("确定")
                .onPositive((dialog12, which) -> {
                    dialog12.dismiss();
                })
                .build();
        dialog.show();
    }

    @OnClick(R2.id.quitButton)
    void quitGroup() {
        if (groupInfo != null && userViewModel.getUserId().equals(groupInfo.owner)) {
            groupViewModel.dismissGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    Intent intent = new Intent(getContext().getPackageName() + ".main");
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "解散群组失败", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            groupViewModel.quitGroup(conversationInfo.conversation.target, Collections.singletonList(0), null).observe(this, aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    Intent intent = new Intent(getContext().getPackageName() + ".main");
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "退出群组失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @OnClick(R2.id.clearMessagesOptionItemView)
    void clearMessage() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.clear_chat_log)
                .content(R.string.clear_chat_log_content)
                .positiveText(R.string.submit2)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
                        dialog.dismiss();
                    }
                })
                .negativeText(R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelable(true)
                .show();

//        if (position == 0) {
//            conversationViewModel.clearConversationMessage(conversationInfo.conversation);
//        } else {
//            conversationViewModel.clearRemoteConversationMessage(conversationInfo.conversation);
//        }
    }

    @OnClick(R2.id.groupQRCodeOptionItemView)
    void showGroupQRCode() {
        Intent intent = QRCodeActivity.buildQRCodeIntent(getActivity(), "群二维码", QRCodeActivity.TYPE_GROUP, groupInfo.target);
        startActivity(intent);
    }

    @OnClick(R2.id.searchMessageOptionItemView)
    void searchGroupMessage() {
        Intent intent = new Intent(getActivity(), SearchMessageActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    @OnClick(R2.id.fileRecordOptionItemView)
    void fileRecord() {
        Intent intent = new Intent(getActivity(), FileRecordActivity.class);
        intent.putExtra("conversation", conversationInfo.conversation);
        startActivity(intent);
    }

    @OnClick(R2.id.portraitImageView)
    void changePortrait() {
        if (getContext() == null) {
            return;
        }
        if (groupInfo.type != GroupInfo.GroupType.Restricted
                || (groupMember.type == GroupMember.GroupMemberType.Manager || groupMember.type == GroupMember.GroupMemberType.Owner)) {
            showSelectPhotoDialog();
        } else {
            Toast.makeText(getContext(), R.string.no_permission, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        if (groupInfo != null && groupInfo.privateChat == 1 && groupMember.type != GroupMember.GroupMemberType.Owner && groupMember.type != GroupMember.GroupMemberType.Manager && !userInfo.uid.equals(groupInfo.owner)) {
            Toast.makeText(getActivity(), R.string.can_not_privateChat, Toast.LENGTH_SHORT).show();
            return;
        }
        if (userInfo.uid.equals(userViewModel.getUserId())) {
            Intent intent = new Intent(getActivity(), PersonalDetailActivity.class);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            intent.putExtra("userInfo", userInfo);
            intent.putExtra("groupId", groupInfo.target);
            startActivity(intent);
        }
    }

    @Override
    public void onAddMemberClick() {
        Intent intent = new Intent(getActivity(), AddGroupMemberActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @Override
    public void onRemoveMemberClick() {
        if (groupInfo != null) {
            Intent intent = new Intent(getActivity(), RemoveGroupMemberActivity.class);
            intent.putExtra("groupInfo", groupInfo);
            startActivity(intent);
        }
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top ? 1 : 0);
        conversationInfo.top = top ? 1 : 0;
    }

    private void markGroup(boolean mark) {
        groupViewModel.setFavGroup(groupInfo.target, mark);
    }

    private void silent(boolean silent) {
        conversationViewModel.setConversationSilent(conversationInfo.conversation, silent);
        conversationInfo.isSilent = silent;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.markGroupSwitchButton) {
            markGroup(isChecked);
        } else if (id == R.id.stickTopSwitchButton) {
            stickTop(isChecked);
        } else if (id == R.id.silentSwitchButton) {
            silent(isChecked);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                Toast.makeText(getActivity(), "更新头像失败: 选取文件失败 ", Toast.LENGTH_SHORT).show();
                return;
            }
            File thumbImgFile = ImageUtils.compressImage(images.get(0).path);
            if (thumbImgFile == null) {
                Toast.makeText(getActivity(), "更新头像失败: 生成缩略图失败", Toast.LENGTH_SHORT).show();
                return;
            }
            updatePhoto(thumbImgFile);
        } else if (requestCode == OPEN_CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // 拍照回传图片
            String path = FileUtils.getPath(getActivity(), imageUri);
            if (path != null) {
                File thumbImgFile = ImageUtils.compressImage(path);
                updatePhoto(thumbImgFile);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // 上传头像
    private void updatePhoto(File file) {
        groupViewModel.uploadGroupPortrait(file).observe(this, response -> {
            if (response.isSuccess()) {
                updateIMGroupPortrait(response.result);

                GlideUtil.load(GroupConversationInfoFragment.this, response.result)
                        .placeholder(R.mipmap.ic_group_default_portrait)
                        .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 4)))
                        .into(portraitImageView);
                Toast.makeText(getActivity(), "上传头像成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "上传头像失败: " + response.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateIMGroupPortrait(String portrait) {
        groupViewModel.modifyGroupInfo(groupInfo.target, ModifyGroupInfoType.Modify_Group_Portrait, portrait, null, Collections.singletonList(0)).observe(this, operateResult -> {
            if (operateResult.isSuccess()) {
                Toast.makeText(getContext(), "修改群头像成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "修改群头像失败: " + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
