/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcIntent;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.InviteFriendActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.group.GroupMemberMessageHistoryActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 用户信息
 */
public class UserInfoFragment extends Fragment {
    @BindView(R2.id.portraitImageView)
    ShapeableImageView portraitImageView;

    @BindView(R2.id.largePortraitLayout)
    ConstraintLayout largePortraitLayout;
    @BindView(R2.id.largePortraitImageView)
    ShapeableImageView largePortraitImageView;

    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.genderTextView)
    TextView genderTextView;
    @BindView(R2.id.accountTextView)
    TextView accountTextView;

    @BindView(R2.id.chatButton)
    View chatButton;
    @BindView(R2.id.voipChatButton)
    View voipChatButton;
    @BindView(R2.id.inviteButton)
    Button inviteButton;
    @BindView(R2.id.aliasOptionItemView)
    OptionItemView aliasOptionItemView;

    @BindView(R2.id.messagesOptionItemView)
    OptionItemView messagesOptionItemView;

    @BindView(R2.id.qrCodeOptionItemView)
    OptionItemView qrCodeOptionItemView;

    @BindView(R2.id.momentButton)
    View momentButton;

    @BindView(R2.id.favContactTextView)
    TextView favContactTextView;

    private UserInfo userInfo;
    private String groupId;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;

    public static UserInfoFragment newInstance(UserInfo userInfo, String groupId) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("userInfo", userInfo);
        if (!TextUtils.isEmpty(groupId)) {
            args.putString("groupId", groupId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        userInfo = args.getParcelable("userInfo");
        groupId = args.getString("groupId");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_info_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        String selfUid = userViewModel.getUserId();
        if (selfUid.equals(userInfo.uid)) {
            // self
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            qrCodeOptionItemView.setVisibility(View.VISIBLE);
            aliasOptionItemView.setVisibility(View.GONE);
        } else if (contactViewModel.isFriend(userInfo.uid)) {
            // friend
            chatButton.setVisibility(View.VISIBLE);
            voipChatButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.GONE);
        } else {
            // stranger
            momentButton.setVisibility(View.GONE);
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.VISIBLE);
            aliasOptionItemView.setVisibility(View.GONE);
        }
        if (userInfo.type == 1) {
            // 机械人
            voipChatButton.setVisibility(View.GONE);
        }
        if (userInfo.uid.equals(Config.FILE_TRANSFER_ID)) {
            chatButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.GONE);
        }

//        setUserInfo(userInfo);
        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), userInfos -> {
            for (UserInfo info : userInfos) {
                if (userInfo.uid.equals(info.uid)) {
                    userInfo = info;
                    setUserInfo(info);
                    break;
                }
            }
        });
//        userViewModel.getUserInfo(userInfo.uid, true);
        asyncUserInfo(userInfo);
        favContactTextView.setVisibility(contactViewModel.isFav(userInfo.uid) ? View.VISIBLE : View.GONE);

        if (!WfcUIKit.getWfcUIKit().isSupportMoment()) {
            momentButton.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(groupId)) {
            messagesOptionItemView.setVisibility(View.VISIBLE);
        } else {
            messagesOptionItemView.setVisibility(View.GONE);
        }
    }

    // 更新 userinfo
    private void asyncUserInfo(UserInfo userInfo) {
        userViewModel.getUserInfoAsync(userInfo.uid, true)
                .observe(getViewLifecycleOwner(), this::setUserInfo);
        userViewModel.getUserInfoEx(userInfo.uid);
    }

    private void setUserInfo(UserInfo userInfo) {
        if (userInfo instanceof NullUserInfo) {
            Toast.makeText(getContext(), userInfo.name, Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }

        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 10)));
        GlideUtil.load(this, ChatManager.Instance().getUserPortrait(userInfo))
                .apply(requestOptions)
                .into(portraitImageView);
        GlideUtil.load(this, ChatManager.Instance().getUserPortrait(userInfo))
                .apply(requestOptions)
                .into(largePortraitImageView);
        if (TextUtils.isEmpty(groupId)) {
            nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo.uid));
            accountTextView.setText("账号 : " + userInfo.memberName);
        } else {
            String name = ChatManager.Instance().getGroupMemberDisplayName(groupId, userInfo.uid);
            nameTextView.setText(name);
            accountTextView.setText("账号 : " + userInfo.memberName);
        }

        String gender = "";
        String[] genders = getResources().getStringArray(cn.wildfire.chat.kit.R.array.gender_spinner_values);
        switch (userInfo.gender) {
            case 1:
                gender = genders[0];
                break;
            case 2:
                gender = genders[1];
                break;
            case 3:
                gender = genders[2];
                break;
        }
        genderTextView.setText("性别 : " + gender);
    }

    @OnClick(R2.id.chatButton)
    void chat() {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        getActivity().finish();
    }

    @OnClick(R2.id.momentButton)
    void moment() {
        Intent intent = new Intent(WfcIntent.ACTION_MOMENT);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.voipChatButton)
    void voipChat() {
        WfcUIKit.singleCall(getActivity(), userInfo.uid, false);
    }

    // 设置昵称或别称 click
    @OnClick(R2.id.aliasOptionItemView)
    void alias() {
        String selfUid = userViewModel.getUserId();
        // 修改个人昵称
        if (selfUid.equals(userInfo.uid)) {
            Intent intent = new Intent(getActivity(), ChangeMyNameActivity.class);
            startActivity(intent);
        } else {
            // 修改备注名称
            Intent intent = new Intent(getActivity(), SetAliasActivity.class);
            intent.putExtra("userId", userInfo.uid);
            startActivity(intent);
        }
    }

    @OnClick(R2.id.messagesOptionItemView)
    void showUserMessages() {
        Intent intent = new Intent(getActivity(), GroupMemberMessageHistoryActivity.class);
        intent.putExtra("groupId", groupId);
        intent.putExtra("groupMemberId", userInfo.uid);
        startActivity(intent);
    }

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    // 打開大圖
    @OnClick(R2.id.portraitImageView)
    void openPortraitImageView() {
        startLargeAnimation();
    }

    private void startLargeAnimation() {
        largePortraitLayout.setVisibility(View.VISIBLE);
        ScaleAnimation scaleAnimation = new ScaleAnimation(-1f, 1f, -1f, 1f);
        scaleAnimation.setDuration(250);

        TranslateAnimation amTranslate = new TranslateAnimation(-100f, 0, -800f, 0);
        amTranslate.setDuration(250);

        AlphaAnimation alphaAnimation = new AlphaAnimation(-1f, 1f);
        alphaAnimation.setDuration(250);

        AnimationSet set = new AnimationSet(false);
        set.addAnimation(scaleAnimation);
        set.addAnimation(amTranslate);
        set.addAnimation(alphaAnimation);
        //將動畫參數設定到圖片並開始執行動畫
        largePortraitImageView.startAnimation(set);
    }

    // 關閉大圖
    @OnClick(R2.id.largePortraitLayout)
    void closeLargePortraitLayout() {
        largePortraitLayout.setVisibility(View.GONE);
    }

    private void updatePortrait() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                Toast.makeText(getActivity(), "更新头像失败: 选取文件失败 ", Toast.LENGTH_SHORT).show();
                return;
            }
            File thumbImgFile = ImageUtils.genThumbImgFile(images.get(0).path);
            if (thumbImgFile == null) {
                Toast.makeText(getActivity(), "更新头像失败: 生成缩略图失败", Toast.LENGTH_SHORT).show();
                return;
            }
//            String imagePath = thumbImgFile.getAbsolutePath();

            MutableLiveData<OperateResult<Boolean>> result = userViewModel.updateUserPortrait(thumbImgFile);
            result.observe(this, booleanOperateResult -> {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(getActivity(), "更新头像成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "更新头像失败: " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R2.id.inviteButton)
    void invite() {
        Intent intent = new Intent(getActivity(), InviteFriendActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
        getActivity().finish();
    }

    @OnClick(R2.id.qrCodeOptionItemView)
    void showMyQRCode() {
        UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        startActivity(QRCodeActivity.buildQRCodeIntent(getActivity(), "二维码", QRCodeActivity.TYPE_PERSON, userInfo.uid));
    }
}
