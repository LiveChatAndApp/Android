/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_Portrait;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.PaymentInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.WalletInfo;
import cn.wildfirechat.model.WalletOrderInfo;
import cn.wildfirechat.model.WebResponse;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.GetUserInfoCallback;
import cn.wildfirechat.remote.OnUserInfoUpdateListener;
import cn.wildfirechat.remote.UserInfoCallback;

public class UserViewModel extends ViewModel implements OnUserInfoUpdateListener {
    private String TAG = getClass().getSimpleName();
    private MutableLiveData<List<UserInfo>> userInfoLiveData;
    private MutableLiveData<WalletInfo> walletLiveData = new MutableLiveData<>();

    public UserViewModel() {
        ChatManager.Instance().addUserInfoUpdateListener(this);
    }

    public static List<UserInfo> getUsers(List<String> ids, String groupId) {
        return ChatManager.Instance().getUserInfos(ids, groupId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ChatManager.Instance().removeUserInfoUpdateListener(this);
    }

    public MutableLiveData<List<UserInfo>> userInfoLiveData() {
        if (userInfoLiveData == null) {
            userInfoLiveData = new MutableLiveData<>();
        }
        return userInfoLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> updateUserPortrait(File file) {
        MutableLiveData<OperateResult<Boolean>> resultLiveData = new MutableLiveData<>();
        if (file != null) {
            ModifyMyInfoEntry entry = new ModifyMyInfoEntry(Modify_Portrait, file);
            ChatManager.Instance().modifyMyInfo(Collections.singletonList(entry), new GeneralCallback() {

                @Override
                public void onSuccess() {
                    resultLiveData.setValue(new OperateResult<Boolean>(true, 0));
                }

                @Override
                public void onFail(int errorCode) {
                    resultLiveData.setValue(new OperateResult<>(errorCode));
                }
            });
        }
        return resultLiveData;
    }

    public MutableLiveData<OperateResult<Boolean>> modifyMyInfo(List<ModifyMyInfoEntry> values) {
        MutableLiveData<OperateResult<Boolean>> result = new MutableLiveData<>();
        ChatManager.Instance().modifyMyInfo(values, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(true, 0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(false, errorCode));
            }
        });
        return result;
    }

    public LiveData<UserInfo> getUserInfoAsync(String userId, boolean refresh) {
        MutableLiveData<UserInfo> data = new MutableLiveData<>();
        ChatManager.Instance().getUserInfoAsync(userId, null, refresh, new UserInfoCallback<UserInfo>() {

            @Override
            public void onUiSuccess(UserInfo userInfo) {
                data.postValue(userInfo);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                NullUserInfo userInfo = new NullUserInfo(userId);
                userInfo.name = msg;
                data.postValue(userInfo);
            }
        });
        return data;
    }

    public UserInfo getUserInfo(String userId, boolean refresh) {
        return ChatManager.Instance().getUserInfo(userId, refresh);
    }

    public UserInfo getUserInfoByIM(String userId, boolean refresh) {
        return ChatManager.Instance().getUserInfoByIM(userId, "", refresh);
    }

    public UserInfo getUserInfo(String userId, String groupId, boolean refresh) {
        return ChatManager.Instance().getUserInfo(userId, groupId, refresh);
    }

    public void getUserInfoEx(String userId) {
        ChatManager.Instance().getUserInfo(userId, true, new GetUserInfoCallback() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                LogHelper.e(TAG, "getUserInfoEx onSuccess uid = " + userId);
            }

            @Override
            public void onFail(int errorCode) {
                LogHelper.e(TAG, "getUserInfoEx onFail uid = " + userId);
            }
        });
    }

    public String getUserDisplayName(UserInfo userInfo) {
        return ChatManager.Instance().getUserDisplayName(userInfo);
    }

    public List<UserInfo> getUserInfos(List<String> userIds) {
        return ChatManager.Instance().getUserInfos(userIds, null);
    }

    public String getUserId() {
        return ChatManager.Instance().getUserId();
    }


    public String getUserSetting(int scope, String key) {
        return ChatManager.Instance().getUserSetting(scope, key);
    }

    public MutableLiveData<OperateResult<Integer>> setUserSetting(int scope, String key, String value) {
        MutableLiveData<OperateResult<Integer>> result = new MutableLiveData<>();
        ChatManager.Instance().setUserSetting(scope, key, value, new GeneralCallback() {
            @Override
            public void onSuccess() {
                result.setValue(new OperateResult<>(0));
            }

            @Override
            public void onFail(int errorCode) {
                result.setValue(new OperateResult<>(errorCode));

            }
        });
        return result;
    }

    @Override
    public void onUserInfoUpdate(List<UserInfo> userInfos) {
        if (userInfoLiveData != null && userInfos != null && !userInfos.isEmpty()) {
            userInfoLiveData.setValue(userInfos);
        }
    }

    public LiveData<WalletInfo> subMyWallet() {
        return walletLiveData;
    }

    /**
     * call get wallet api
     */
    public void getMyWallet() {
        ImplementUserSource.Instance().getMyWallet(new UserInfoCallback<WalletInfo>() {

            @Override
            public void onUiSuccess(WalletInfo info) {
                walletLiveData.postValue(info);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WalletInfo info = new WalletInfo();
                info.error = true;
                walletLiveData.postValue(info);
            }
        });
    }

    /**
     * 增加提款方式
     *
     * @param channel
     * @param file
     * @param account
     * @param bankName
     * @param owner
     * @param customName
     * @return
     */
    public LiveData<String> addPaymentMethod(int channel, File file, String account, String bankName, String owner, String customName) {
//        info 装：
//        帳號         channel
//        銀行名稱       info
//        收款人        name
        MutableLiveData<String> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().addPaymentMethod(channel, file, account, bankName, owner, customName, new UserInfoCallback<String>() {

            @Override
            public void onUiSuccess(String result) {
                liveData.postValue("done");
            }

            @Override
            public void onUiFailure(int code, String msg) {
                liveData.postValue(msg);
            }
        });
        return liveData;
    }

    /**
     * 删除提款方式
     *
     * @param paymentMethodId
     * @return
     */
    public LiveData<String> deletePaymentMethod(int paymentMethodId) {
        MutableLiveData<String> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().deletePaymentMethod(paymentMethodId, new UserInfoCallback<String>() {

            @Override
            public void onUiSuccess(String result) {
                liveData.postValue("done");
            }

            @Override
            public void onUiFailure(int code, String msg) {
                liveData.postValue(msg);
            }
        });
        return liveData;
    }

    /**
     * 取得提款渠道
     *
     * @param isSpinner
     * @return
     */
    public LiveData<List<PaymentInfo>> getPaymentMethod(boolean isSpinner) {
        MutableLiveData<List<PaymentInfo>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().getPaymentMethod(new UserInfoCallback<List<PaymentInfo>>() {

            @Override
            public void onUiSuccess(List<PaymentInfo> list) {
                if (isSpinner) {
                    PaymentInfo info = new PaymentInfo();
                    info.name = "选择银行卡";
                    list.add(0, info);
                }
                liveData.postValue(list);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                liveData.postValue(null);
            }
        });
        return liveData;
    }

    // 提现订单
    public LiveData<WebResponse<String>> withdrawApply(int channel, double amount, String currency, int paymentMethodId, String tradePwd) {
        MutableLiveData<WebResponse<String>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().withdrawApply(channel, amount, currency, paymentMethodId, tradePwd, new UserInfoCallback<WebResponse<String>>() {

            @Override
            public void onUiSuccess(WebResponse<String> result) {
                liveData.postValue(result);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WebResponse<String> response = new WebResponse<>();
                response.code = code;
                response.message = msg;
                liveData.postValue(response);
            }
        });
        return liveData;
    }

    public WalletInfo getWalletInfo() {
        return walletLiveData.getValue();
    }

    /**
     * 取得錢包明細資料
     *
     * @return
     */
    public LiveData<WebResponse<List<WalletOrderInfo>>> getWalletDetail() {
        MutableLiveData<WebResponse<List<WalletOrderInfo>>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().getWalletOrderList(new SimpleCallback<List<WalletOrderInfo>>() {
            @Override
            public void onUiSuccess(List<WalletOrderInfo> list) {
                WebResponse<List<WalletOrderInfo>> response = new WebResponse<>();
                response.code = 0;
                response.message = "success";
                response.result = list;
                liveData.postValue(response);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WebResponse<List<WalletOrderInfo>> response = new WebResponse<>();
                response.code = code;
                response.message = msg;
                liveData.postValue(response);
            }
        });
        return liveData;
    }

    public boolean checkFriend(String userId) {
        return ChatManager.Instance().isMyFriend(userId);
    }
}
