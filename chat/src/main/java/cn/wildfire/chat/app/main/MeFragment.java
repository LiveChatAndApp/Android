/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.app.login.LoginActivity;
import cn.wildfire.chat.app.setting.AccountActivity;
import cn.wildfire.chat.app.setting.ResetPasswordActivity;
import cn.wildfire.chat.app.setting.SettingActivity;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.conversation.file.FileRecordListActivity;
import cn.wildfire.chat.kit.favorite.FavoriteListActivity;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.settings.MessageNotifySettingActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.PersonalDetailActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.user.wallet.MyWalletActivity;
import cn.wildfire.chat.kit.utils.GlideUtil;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

/**
 * 我的个人中心
 */
public class MeFragment extends Fragment {
    String TAG = getClass().getSimpleName();

    @BindView(R.id.meLinearLayout)
    LinearLayout meLinearLayout;
    @BindView(R.id.largePortraitLayout)
    ConstraintLayout largePortraitLayout;
    @BindView(R.id.largePortraitImageView)
    ShapeableImageView largePortraitImageView;

    @BindView(R.id.portraitImageView)
    ShapeableImageView portraitImageView;
    @BindView(R.id.nameTextView)
    TextView nameTextView;
    @BindView(R.id.accountTextView)
    TextView accountTextView;
    @BindView(R.id.genderTextView)
    TextView genderTextView;

    @BindView(R.id.notificationOptionItemView)
    OptionItemView notificationOptionItem;

    @BindView(R.id.settintOptionItemView)
    OptionItemView settingOptionItem;

    @BindView(R.id.fileRecordOptionItemView)
    OptionItemView fileRecordOptionItem;

    @BindView(R.id.myWalletOptionItemView)
    OptionItemView myWalletOptionItemView;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    private Observer<List<UserInfo>> userInfoLiveDataObserver = new Observer<List<UserInfo>>() {
        @Override
        public void onChanged(@Nullable List<UserInfo> userInfos) {
            if (userInfos == null) {
                return;
            }
            for (UserInfo info : userInfos) {
                if (info.uid.equals(userViewModel.getUserId())) {
                    userInfo = info;
                    updateUserInfo(userInfo);
                    break;
                }
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment_me, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            asyncUserInfo();
            setTitle();
        }
    }

    private void setTitle() {
        if (userInfo != null) {
            setTitle(userInfo.nickName);
        }
    }

    private void setTitle(String title) {
        if (getUserVisibleHint()) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        asyncUserInfo();
    }

    private void updateUserInfo(UserInfo userInfo) {
        RequestOptions options = new RequestOptions()
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 10)));
        GlideUtil.load(this, userInfo.avatar)
                .apply(options)
                .into(portraitImageView);
        GlideUtil.load(this, userInfo.avatar)
                .apply(options)
                .into(largePortraitImageView);
        nameTextView.setText(userInfo.nickName);
        accountTextView.setText(String.format(getString(R.string.my_chat_account), userInfo.memberName));
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
        genderTextView.setText(String.format(getString(R.string.my_gender), gender));
        myWalletOptionItemView.setDesc(String.format(getString(R.string.my_balance), "" + userInfo.balance));
        setTitle(userInfo.nickName);
    }

    private void init() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
//        userViewModel.userInfoLiveData().observeForever(userInfoLiveDataObserver);
//        if (ChatManager.Instance().isCommercialServer()) {
//            fileRecordOptionItem.setVisibility(View.VISIBLE);
//        } else {
//            fileRecordOptionItem.setVisibility(View.GONE);
//        }
    }

    // 更新 userinfo
    private void asyncUserInfo() {
        if (userViewModel == null)
            return;
        userViewModel.getUserInfoAsync(userViewModel.getUserId(), true)
                .observe(getViewLifecycleOwner(), info -> {
                    userInfo = info;
                    if (userInfo != null) {
                        SharedPreferences sp2 = getContext().getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                        sp2.edit()
                                .putBoolean("createGroupEnable", userInfo.createGroupEnable)
                                .apply();
                        LogHelper.e(TAG, "getUserInfoAsync createGroupEnable = " + userInfo.createGroupEnable);
                        updateUserInfo(userInfo);
                    } else {
                        asyncUserInfo();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userViewModel.userInfoLiveData().removeObserver(userInfoLiveDataObserver);
    }

    // 我的信息
    @OnClick(R.id.meLinearLayout)
    void showMyInfo() {
        Intent intent = new Intent(getActivity(), PersonalDetailActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    // 收藏
    @OnClick(R.id.favOptionItemView)
    void fav() {
        Intent intent = new Intent(getActivity(), FavoriteListActivity.class);
        startActivity(intent);
    }

    // 账号与安全
    @OnClick(R.id.accountOptionItemView)
    void account() {
        if (userInfo == null) {
            Toast.makeText(getContext(), R.string.no_user_info, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(getActivity(), AccountActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @OnClick(R.id.myWalletOptionItemView)
    void myWalletClick() {
        Intent intent = new Intent(getActivity(), MyWalletActivity.class);
        intent.putExtra("userInfo", userInfo);
        Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(getActivity(),
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        startActivity(intent, bundle);
    }

    @OnClick(R.id.fileRecordOptionItemView)
    void files() {
        Intent intent = new Intent(getActivity(), FileRecordListActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.themeOptionItemView)
    void theme() {
        SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
        boolean darkTheme = sp.getBoolean("darkTheme", true);
        new MaterialDialog.Builder(getContext()).items(R.array.themes).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0 && darkTheme) {
                    sp.edit().putBoolean("darkTheme", false).apply();
                    restart();
                    return;
                }
                if (position == 1 && !darkTheme) {
                    sp.edit().putBoolean("darkTheme", true).apply();
                    restart();
                }
            }
        }).show();
    }

    private void restart() {
        Intent i = getActivity().getApplicationContext().getPackageManager().getLaunchIntentForPackage(getActivity().getApplicationContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    // 设置
    @OnClick(R.id.settintOptionItemView)
    void setting() {
        Intent intent = new Intent(getActivity(), SettingActivity.class);
        startActivity(intent);
    }

    // 消息与通知
    @OnClick(R.id.notificationOptionItemView)
    void msgNotifySetting() {
        Intent intent = new Intent(getActivity(), MessageNotifySettingActivity.class);
        startActivity(intent);
    }

    // 打開大圖
    @OnClick(R.id.portraitImageView)
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
    @OnClick(R.id.largePortraitLayout)
    void closeLargePortraitLayout() {
        largePortraitLayout.setVisibility(View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.me, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.qr_code:
                UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
                startActivity(QRCodeActivity.buildQRCodeIntent(getActivity(), "二维码", QRCodeActivity.TYPE_PERSON, userInfo.uid));
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
