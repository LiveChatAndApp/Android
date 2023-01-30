/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
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
import cn.wildfire.chat.kit.GetPhotoFragment;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 个人用户信息
 */
public class PersonalDetailFragment extends GetPhotoFragment {
    @BindView(R2.id.portraitImageView)
    ShapeableImageView portraitImageView;

    @BindView(R2.id.iconRelativeLayout)
    ConstraintLayout iconRelativeLayout;

    @BindView(R2.id.accountOptionItemView)
    OptionItemView accountOptionItemView;
    @BindView(R2.id.aliasOptionItemView)
    OptionItemView aliasOptionItemView;
    @BindView(R2.id.genderOptionItemView)
    OptionItemView genderOptionItemView;
    @BindView(R2.id.phoneOptionItemView)
    OptionItemView phoneOptionItemView;

    @BindView(R2.id.qrCodeOptionItemView)
    OptionItemView qrCodeOptionItemView;

    private UserInfo userInfo;
    private String groupId;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;

    boolean isUpdatePhoto = false;

    public static PersonalDetailFragment newInstance(UserInfo userInfo, String groupId) {
        PersonalDetailFragment fragment = new PersonalDetailFragment();
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
        View view = inflater.inflate(R.layout.personal_detail_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 上传头像时，别更新资料
        if (!isUpdatePhoto) {
            asyncUserInfo();
            ChatManager.Instance().getMainHandler().postDelayed(this::asyncUserInfo, 1000);
        }
    }

    private void init() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        setUserInfo(userInfo);
//        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), userInfos -> {
//            for (UserInfo info : userInfos) {
//                if (userInfo.uid.equals(info.uid)) {
//                    userInfo = info;
//                    setUserInfo(userInfo);
//                    break;
//                }
//            }
//        });
    }

    private void asyncUserInfo() {
        userViewModel.getUserInfoAsync(userInfo.uid, true).observe(getViewLifecycleOwner(), new Observer<UserInfo>() {
            @Override
            public void onChanged(UserInfo info) {
                if (userInfo.uid.equals(info.uid)) {
                    userInfo = info;
                    setUserInfo(userInfo);
                }
            }
        });
    }

    private void setUserInfo(UserInfo userInfo) {
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 10)));

        GlideUtil.load(this, userInfo.avatar)
                .apply(requestOptions)
                .into(portraitImageView);

        accountOptionItemView.setDesc(userInfo.memberName);
        aliasOptionItemView.setDesc(userInfo.nickName);
        phoneOptionItemView.setDesc(TextUtils.isEmpty(userInfo.mobile) ? "" : userInfo.mobile);
        String gender = "";
        String[] genders = getResources().getStringArray(R.array.gender_spinner_values);
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
        genderOptionItemView.setDesc(gender);

    }

    // 性别 click
    @OnClick(R2.id.genderOptionItemView)
    void genderClick() {
        Intent resetPasswordIntent = new Intent(getActivity(), ChangeGenderActivity.class);
        startActivity(resetPasswordIntent);
    }

    // 昵称 click
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


    // 修改头像
    @OnClick(R2.id.iconRelativeLayout)
    void portrait() {
        if (getContext() == null) {
            return;
        }
        showSelectPhotoDialog();
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

    private void compressPic(File file) {
        File cacheFile = new File(Environment.getExternalStorageDirectory().getPath() + "/GameTv/player/tmp");
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }

        if (cacheFile.exists()) {
            cacheFile.mkdirs();
        }

//        Luban.compress(file, cacheFile)
//                .setMaxSize(100)
//                .setMaxWidth(500)
//                .setMaxHeight(500)// 传入要压缩的图片
//                .launch(new OnCompressListener() {
//                    @Override
//                    public void onStart() {
//                        // 压缩开始前调用，可以在方法内启动 loading UI
//                    }
//
//                    @Override
//                    public void onSuccess(File file) {
//                        // 压缩成功后调用，返回压缩后的图片文件
//                        uploadFile(file);
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        //当压缩过程出现问题时调用
//                    }
//                });
    }

    // 上传头像
    private void updatePhoto(File file) {
        isUpdatePhoto = true;
        userViewModel.updateUserPortrait(file).observe(this, booleanOperateResult -> {
            isUpdatePhoto = false;
            if (booleanOperateResult.isSuccess()) {
                asyncUserInfo();
                Toast.makeText(getActivity(), "更新头像成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "更新头像失败: " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R2.id.qrCodeOptionItemView)
    void showMyQRCode() {
        UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        startActivity(QRCodeActivity.buildQRCodeIntent(getActivity(), "二维码", QRCodeActivity.TYPE_PERSON, userInfo.uid));
    }
}
