/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.imageview.ShapeableImageView;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GetPhotoFragment;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.AddGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.NewRemoveGroupMemberActivity;
import cn.wildfire.chat.kit.group.SetGroupNameActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 设定群聊资讯
 */
public class CreateGroupFragment extends GetPhotoFragment implements CreateGroupAdapter.OnMemberClickListener {

    private int REQUEST_MODIFY_ADD_MEMBER = 1000;
    private int REQUEST_MODIFY_REMOVE_MEMBER = 1002;
    private int REQUEST_MODIFY_NAME = 1001;

    @BindView(R2.id.contentNestedScrollView)
    NestedScrollView contentNestedScrollView;

    @BindView(R2.id.portraitImageView)
    ShapeableImageView portraitImageView;
    // group
    @BindView(R2.id.groupNameOptionItemView)
    OptionItemView groupNameOptionItemView;
    // common
    @BindView(R2.id.memberRecyclerView)
    RecyclerView memberReclerView;

    private CreateGroupAdapter conversationMemberAdapter;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;

    private GroupViewModel groupViewModel;

    private ArrayList<UserInfo> userInfos = new ArrayList<>();
    private String portraitUrl = "";

    public static CreateGroupFragment newInstance(ArrayList<UserInfo> list) {
        CreateGroupFragment fragment = new CreateGroupFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("userInfos", list);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        userInfos.addAll(args.getParcelableArrayList("userInfos"));
        // add me
        String meId = ChatManager.Instance().getUserId();
        UserInfo meUserInfo = ChatManager.Instance().getUserInfo(meId, false);
        userInfos.add(meUserInfo);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.group_build, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.build) {
            buildGroup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_group_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void init() {
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        // 设定群组预设名称
        groupNameOptionItemView.setDesc(getDefaultGroupName());
        showGroupMembers();
    }

    // 取得群组预设名称
    private String getDefaultGroupName() {
        String groupName = "";
        for (int i = 0; i < 3 && i < userInfos.size(); i++) {
            groupName += userViewModel.getUserDisplayName(userInfos.get(i)) + "、";
        }
        groupName = groupName.substring(0, groupName.length() - 1);
        if (userInfos.size() > 3) {
            groupName += " ...";
        }

        return groupName.substring(0, groupName.length() - 1);
    }

    private void showGroupMembers() {
        if (userInfos == null || userInfos.isEmpty()) {
            return;
        }

        conversationMemberAdapter = new CreateGroupAdapter();
        conversationMemberAdapter.setMembers(userInfos);
        conversationMemberAdapter.setOnMemberClickListener(this);
        memberReclerView.setAdapter(conversationMemberAdapter);
        memberReclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        memberReclerView.setNestedScrollingEnabled(false);
        memberReclerView.setHasFixedSize(true);
        memberReclerView.setFocusable(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MODIFY_ADD_MEMBER && resultCode == Activity.RESULT_OK) {
            List<UserInfo> list = data.getParcelableArrayListExtra("userInfos");
            conversationMemberAdapter.addMembers(list);
        } else if (requestCode == REQUEST_MODIFY_REMOVE_MEMBER && resultCode == Activity.RESULT_OK) {
            List<String> list = data.getStringArrayListExtra("userInfos");
            conversationMemberAdapter.removeMembers(list);
        } else if (requestCode == REQUEST_MODIFY_NAME && resultCode == Activity.RESULT_OK) {
            String groupName = data.getStringExtra("name");
            groupNameOptionItemView.setDesc(groupName);
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
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

    @OnClick(R2.id.groupNameOptionItemView)
    void updateGroupName() {
        Intent intent = new Intent(getActivity(), SetGroupNameActivity.class);
        intent.putExtra("name", groupNameOptionItemView.getDesc());
        startActivityForResult(intent, REQUEST_MODIFY_NAME);
    }

    @OnClick(R2.id.portraitImageView)
    void changePortrait() {
        if (getContext() == null) {
            return;
        }
        showSelectPhotoDialog();
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @Override
    public void onAddMemberClick() {
        Intent intent = new Intent(getActivity(), AddGroupMemberActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        intent.putParcelableArrayListExtra("userInfos", userInfos);
        startActivityForResult(intent, REQUEST_MODIFY_ADD_MEMBER, bundle);
    }

    @Override
    public void onRemoveMemberClick() {
        Intent intent = new Intent(getActivity(), NewRemoveGroupMemberActivity.class);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getContext(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        intent.putParcelableArrayListExtra("userInfos", userInfos);
        startActivityForResult(intent, REQUEST_MODIFY_REMOVE_MEMBER, bundle);
    }

    private void buildGroup() {
        List<UserInfo> list = conversationMemberAdapter.getMembers();
        // 两人以下则不能开启
        if (list.size() <=2) {
            Toast.makeText(getContext(), R.string.member_too_less, Toast.LENGTH_SHORT).show();
            return;
        }
        // 超过两人
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .content("创建中...")
                .progress(true, 100)
                .build();
        dialog.show();
        groupViewModel.createGroup(groupNameOptionItemView.getDesc(), portraitUrl, list, null, Arrays.asList(0)).observe(this, result -> {
            dialog.dismiss();
            if (result.isSuccess()) {
                getActivity().setResult(Activity.RESULT_OK);
                Toast.makeText(getActivity(), getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
            }
            getActivity().finish();
        });
    }

    // 上传头像
    private void updatePhoto(File file) {
        groupViewModel.uploadGroupPortrait(file).observe(this, response -> {
            if (response.isSuccess()) {
                portraitUrl = response.result;
                GlideUtil.load(this, portraitUrl)
                        .placeholder(R.mipmap.ic_group_default_portrait)
                        .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 4)))
                        .into(portraitImageView);
                Toast.makeText(getActivity(), "更新头像成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "更新头像失败: " + response.message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
