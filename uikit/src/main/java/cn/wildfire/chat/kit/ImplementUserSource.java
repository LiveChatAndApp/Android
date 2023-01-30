package cn.wildfire.chat.kit;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;
import cn.wildfire.chat.kit.qrcode.GetStringResult;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.utils.Security;
import cn.wildfirechat.UserSource;
import cn.wildfirechat.model.AppServerFriendRequest;
import cn.wildfirechat.model.CustomChatRoomInfo;
import cn.wildfirechat.model.GroupPageInfo;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.NullUserInfo;
import cn.wildfirechat.model.PaymentInfo;
import cn.wildfirechat.model.RechargeChannel;
import cn.wildfirechat.model.RechargeResultInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.WalletInfo;
import cn.wildfirechat.model.WalletOrderInfo;
import cn.wildfirechat.model.WebResponse;
import cn.wildfirechat.remote.SearchUserCallback;
import cn.wildfirechat.remote.UserInfoCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * 不透过 IM server ,
 * 实作 我方用户资料server api
 */
public class ImplementUserSource implements UserSource {
    private String TAG = "ImplementUserSource";
    private Gson gson = new Gson();
    private static ImplementUserSource Instance = new ImplementUserSource();

    public static String IMAGE_HOST = "";
    public static String APP_SERVER_ADDRESS = Security.decrypt(BuildConfig.API_HOST);

    //Platform_iOS = 1,
    //Platform_Android = 2,
    //Platform_Windows = 3,
    //Platform_OSX = 4,
    //Platform_WEB = 5,
    //Platform_WX = 6,
    //Platform_linux = 7,
    //Platform_iPad = 8,
    //Platform_APad = 9,
    //如果是android pad设备，需要改这里，另外需要在ClientService对象中修改设备类型，请在ClientService代码中搜索"android pad"
    //if（当前设备是android pad)
    //  params.put("platform", new Integer(9));
    //else

    private ImplementUserSource() {

    }

    public static ImplementUserSource Instance() {
        return Instance;
    }

    public void setImageHost(String host) {
        IMAGE_HOST = host;
    }

    public String getImageHost() {
        return IMAGE_HOST;
    }

    /**
     * 取得个人信息
     *
     * @param userId
     * @param callback
     * @return
     */
    @Override
    public UserInfo getUser(String userId, UserInfoCallback<UserInfo> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/info";
            Map<String, String> params = new HashMap<>();
            params.put("userId", userId);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            OKHttpHelper.get(url, params, new SimpleCallback<UserInfo>() {
                @Override
                public void onUiSuccess(UserInfo userInfo) {
                    userInfo.portrait = userInfo.avatar;
                    userInfo.displayName = userInfo.nickName;
                    callback.onUiSuccess(userInfo);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NullUserInfo(userId);
    }

    @Override
    public void searchUser(String keyword, SearchUserCallback callback) {

    }

    /**
     * 修改个人信息
     *
     * @param values
     * @param callback
     */
    @Override
    public void modifyMyInfo(List<ModifyMyInfoEntry> values, UserInfoCallback<UserInfo> callback) {
        try {
            File file = null;
            String url = APP_SERVER_ADDRESS + "/info/update/";
            Map<String, Object> params = new HashMap<>();
            int flag = 0;
            for (ModifyMyInfoEntry infoEntry : values) {
                switch (infoEntry.type.getValue()) {
                    case 0:
                        params.put("nickName", infoEntry.value);
                        flag += 1;
                        break;
                    case 1:
                        file = infoEntry.file;
                        flag += 2;
                        break;
                    case 2:
                        params.put("gender", infoEntry.value);
                        flag += 4;
                        break;
                    case 3:
                        params.put("phone", infoEntry.value);
                        flag += 8;
                        break;
                    case 4:
                        params.put("email", infoEntry.value);
                        flag += 10;
                        break;
                    case 5:
                        params.put("address", infoEntry.value);
                        flag += 20;
                        break;
                    case 6:
                        params.put("company", infoEntry.value);
                        flag += 40;
                        break;
                    case 7:
                        params.put("social", infoEntry.value);
                        flag += 80;
                        break;
                    case 8:
                        params.put("extra", infoEntry.value);
                        flag += 100;
                        break;
                    case 9:
                        params.put("name", infoEntry.value);
                        flag += 200;
                        break;
                }
            }
            url = APP_SERVER_ADDRESS + "/info/update/" + hex_to_binary(String.valueOf(flag));
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.formBodyPost(url, params, "avatar", file, new SimpleCallback<UserInfo>() {

                @Override
                public void onUiSuccess(UserInfo userInfo) {
                    callback.onUiSuccess(userInfo);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void qrcode(SimpleCallback<GetStringResult> callback) {
        String url = APP_SERVER_ADDRESS + "/info/qrcode";
        Map<String, String> params = new HashMap<>();
        params.put("platform", new Integer(2).toString());
        try {
            params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
        } catch (Exception e) {
            e.printStackTrace();
            callback.onUiFailure(-1, "网络出来问题了。。。");
            return;
        }
        OKHttpHelper.get(url, params, callback);
    }

    /**
     * 我的钱包 /info/asserts
     */
    public WalletInfo getMyWallet(UserInfoCallback<WalletInfo> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/info/asserts";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            OKHttpHelper.executeGet(url, params).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFailure(-1, e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        // 打印 Header log
                        if (BuildConfig.DEBUG) {
                            printRequestHeader(response);
                        }
                        if (!response.isSuccessful()) {
                            int errorCode = response.code();
                            callback.onFailure(errorCode, "" + response.message());
                            LogHelper.i(TAG, "result = \n Error " + errorCode);
                            return;
                        }
                        try {
                            JsonObject body = gson.fromJson(response.body().string(), JsonObject.class);
                            LogHelper.i(TAG, "url = " + url + "\nresult = \n" + body.toString());
                            JsonElement result = body.get("result");
                            if (result == null) {
                                callback.onFailure(-1, "error result null");
                                return;
                            } else {
                                JsonArray array = result.getAsJsonArray();
                                if (array.size() > 0) {
                                    JsonElement item = array.get(0);
                                    WalletInfo info = new Gson().fromJson(item, WalletInfo.class);
                                    callback.onUiSuccess(info);
                                } else {
                                    WalletInfo info = new WalletInfo();
                                    info.error = true;
                                    info.balance = "0";
                                    callback.onUiSuccess(info);
                                }
                            }

//                            callback.onUiSuccess(userInfo);
                        } catch (Exception e) {
                            callback.onFailure(-1, e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 收款方式列表 /withdraw/payment_method
     */
    public void getPaymentMethod(UserInfoCallback<List<PaymentInfo>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/withdraw/payment_method";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            OKHttpHelper.executeGet(url, params).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFailure(-1, e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        // 打印 Header log
                        if (BuildConfig.DEBUG) {
                            printRequestHeader(response);
                        }
                        if (!response.isSuccessful()) {
                            int errorCode = response.code();
                            callback.onFailure(errorCode, "" + response.message());
                            LogHelper.i(TAG, "result = \n Error " + errorCode);
                            return;
                        }
                        try {
                            JsonObject body = gson.fromJson(response.body().string(), JsonObject.class);
                            LogHelper.i(TAG, "url = " + url + "\nresult = \n" + body.toString());
                            JsonElement result = body.get("result");
                            if (result == null) {
                                callback.onFailure(-1, "error result null");
                                return;
                            } else {
                                JsonArray array = result.getAsJsonArray();
                                if (array.size() > 0) {
                                    List<PaymentInfo> list = new ArrayList<>();
                                    JsonObject item;
                                    for (int i = 0; i < array.size(); i++) {
                                        try {
                                            item = array.get(i).getAsJsonObject();
                                            PaymentInfo info = gson.fromJson(item, PaymentInfo.class);
                                            JsonObject bankData = gson.fromJson(info.info, JsonObject.class);
                                            info.bankName = bankData.get("bankName").getAsString();
                                            info.bankCardNumber = bankData.get("bankCardNumber").getAsString();
                                            info.ownerName = bankData.get("name").getAsString();
                                            list.add(info);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    callback.onUiSuccess(list);
                                } else {
                                    callback.onFailure(-999, "empty");
                                }
                            }
                        } catch (Exception e) {
                            callback.onFailure(-1, e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 提现订单
     * channel 1 银行卡  2 微信   3 支付宝
     *
     * @param channel         收款方式
     * @param amount          提款金额
     * @param currency        币值
     * @param paymentMethodId 提款方式id
     * @param callback
     */
    public void withdrawApply(int channel, double amount, String currency, int paymentMethodId, String tradePwd, UserInfoCallback<WebResponse<String>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/withdraw/apply";
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amount);
            params.put("channel", channel);
            params.put("currency", currency);
            params.put("paymentMethodId", paymentMethodId);
            params.put("tradePwd", Security.getSha256(tradePwd));
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.post(url, params, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    WebResponse<String> response = WebResponse.createSuccess();
                    response.result = s;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * channel 1 银行卡  2 微信   3 支付宝
     *
     * @param file       QR code
     * @param channel    收款方式
     * @param account    銀行名稱 银行账号 收款人姓名
     * @param bankName   銀行名稱 银行账号 收款人姓名
     * @param owner      銀行名稱 银行账号 收款人姓名
     * @param customName 银行卡名称
     * @param callback
     */
    public void addPaymentMethod(int channel, File file, String account, String bankName, String owner, String customName, UserInfoCallback<String> callback) {
        try {
            JsonObject jsonObjectInfo = new JsonObject();
            jsonObjectInfo.addProperty("bankCardNumber", account);
            jsonObjectInfo.addProperty("bankName", bankName);
            jsonObjectInfo.addProperty("name", owner);
            String info = jsonObjectInfo.toString();

            String url = APP_SERVER_ADDRESS + "/withdraw/payment_method/add";
            Map<String, Object> params = new HashMap<>();

            params.put("channel", channel);
            params.put("info", info);
            params.put("name", customName);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.formBodyPost(url, params, "file", file, new SimpleCallback<String>() {

                @Override
                public void onUiSuccess(String result) {
                    callback.onUiSuccess(result);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 移除提币收款方式
     *
     * @param paymentMethodId 提款方式id
     * @param callback
     */
    public void deletePaymentMethod(int paymentMethodId, UserInfoCallback<String> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/withdraw/payment_method/remove/" + paymentMethodId;
            Map<String, Object> params = new HashMap<>();
            params.put("methodId", paymentMethodId);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.post(url, params, new SimpleCallback<String>() {

                @Override
                public void onUiSuccess(String result) {
                    callback.onUiSuccess(result);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询好友邀请列表
     */
    public void getFriendRequest(SimpleCallback<List<AppServerFriendRequest>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/relate/friend/request";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            OKHttpHelper.executeGet(url, params).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFailure(-1, e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        // 打印 Header log
                        if (BuildConfig.DEBUG) {
                            printRequestHeader(response);
                        }
                        if (!response.isSuccessful()) {
                            int errorCode = response.code();
                            callback.onFailure(errorCode, "" + response.message());
                            LogHelper.i(TAG, "result = \n Error " + errorCode);
                            return;
                        }
                        try {
                            JsonObject body = gson.fromJson(response.body().string(), JsonObject.class);
                            LogHelper.i(TAG, "result = \n" + body.toString());
                            JsonArray result = body.getAsJsonArray("result");


                            Type userListType = new TypeToken<ArrayList<AppServerFriendRequest>>() {
                            }.getType();
                            ArrayList<AppServerFriendRequest> list = gson.fromJson(result, userListType);
                            callback.onUiSuccess(list);
                        } catch (Exception e) {
                            callback.onFailure(-1, e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * 发送好友邀请
     *
     * @param uid
     * @param verify     是否需要验证讯息, false: 不需验证, true: 需要验证
     * @param verifyText 验证讯息
     * @param message    自我介绍讯息
     */
    public void sendFriendRequest(String uid, boolean verify, String verifyText, String message, SimpleCallback<WebResponse<String>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/relate/friend/invite";
            Map<String, Object> params = new HashMap<>();
            params.put("uid", uid);
            params.put("verify", verify);
            params.put("verifyText", verifyText);
            params.put("helloText", message);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.post(url, params, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    WebResponse<String> response = WebResponse.createSuccess();
                    response.result = s;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 回应好友邀请
     *
     * @param uid
     * @param verifyText 验证讯息
     */
    public void responseFriendRequest(String uid, int reply, String verifyText, SimpleCallback<WebResponse<String>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/relate/friend/response";
            Map<String, Object> params = new HashMap<>();
            params.put("uid", uid);
            params.put("reply", reply);
            params.put("verifyText", verifyText);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.post(url, params, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    WebResponse<String> response = WebResponse.createSuccess();
                    response.result = s;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 充值渠道列表
     */
    public void getRechargeChannel(String type, SimpleCallback<List<RechargeChannel>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/recharge/channel";
            Map<String, String> params = new HashMap<>();
            params.put("paymentMethod", type);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            OKHttpHelper.executeGet(url, params).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFailure(-1, e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        // 打印 Header log
                        if (BuildConfig.DEBUG) {
                            printRequestHeader(response);
                        }
                        if (!response.isSuccessful()) {
                            int errorCode = response.code();
                            callback.onFailure(errorCode, "" + response.message());
                            LogHelper.i(TAG, "result = \n Error " + errorCode);
                            return;
                        }
                        try {
                            JsonObject body = gson.fromJson(response.body().string(), JsonObject.class);
                            LogHelper.i(TAG, "result = \n" + body.toString());
                            JsonArray result = body.getAsJsonArray("result");


                            Type userListType = new TypeToken<ArrayList<RechargeChannel>>() {
                            }.getType();
                            ArrayList<RechargeChannel> list = gson.fromJson(result, userListType);
                            callback.onUiSuccess(list);
                        } catch (Exception e) {
                            callback.onFailure(-1, e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * 充值申请
     *
     * @param amount
     * @param channelId
     * @param currency
     * @param method
     */
    public void sendRechargeOrder(long amount, long channelId, String currency, int method, SimpleCallback<RechargeResultInfo> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/recharge/apply";
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amount);
            params.put("channelId", channelId);
            params.put("currency", currency);
            params.put("method", method);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.post(url, params, new SimpleCallback<RechargeResultInfo>() {

                @Override
                public void onUiSuccess(RechargeResultInfo result) {
                    callback.onUiSuccess(result);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 我的钱包-订单明細
     */
    public void getWalletOrderList(SimpleCallback<List<WalletOrderInfo>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/info/orderList";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            OKHttpHelper.executeGet(url, params).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.onFailure(-1, e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        // 打印 Header log
                        if (BuildConfig.DEBUG) {
                            printRequestHeader(response);
                        }
                        if (!response.isSuccessful()) {
                            int errorCode = response.code();
                            callback.onFailure(errorCode, "" + response.message());
                            LogHelper.i(TAG, "result = \n Error " + errorCode);
                            return;
                        }
                        try {
                            JsonObject body = gson.fromJson(response.body().string(), JsonObject.class);
                            LogHelper.i(TAG, "result = \n" + body.toString());
                            JsonArray result = body.getAsJsonArray("result");
                            Type userListType = new TypeToken<ArrayList<WalletOrderInfo>>() {
                            }.getType();
                            ArrayList<WalletOrderInfo> list = gson.fromJson(result, userListType);
                            callback.onUiSuccess(list);
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onFailure(-1, e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    public void uploadRechargeScreenshot(int id, File file, SimpleCallback<String> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/recharge/confirm";
            Map<String, Object> params = new HashMap<>();

            params.put("id", id);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            OKHttpHelper.formBodyPost(url, params, "payImageFile", file, new SimpleCallback<String>() {

                @Override
                public void onUiSuccess(String result) {
                    callback.onUiSuccess(result);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传群组头像
     */
    public void uploadGroupPortrait(File portraitFile, SimpleCallback<WebResponse<String>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/group/updatePortrait";
            Map<String, Object> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            OKHttpHelper.formBodyPost(url, params, "file", portraitFile, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String s) {
                    WebResponse<String> response = WebResponse.createSuccess();
                    response.result = s;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取得我的群组信息
     */
    public void getGroupList(int group, int page, int pageOfSize, SimpleCallback<WebResponse<GroupPageInfo>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/group/list";
            Map<String, String> params = new HashMap<>();

            params.put("groupType", "" + group);
            params.put("page", "" + page);
            params.put("pageOfSize", "" + pageOfSize);
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            OKHttpHelper.get(url, params, new SimpleCallback<GroupPageInfo>() {
                @Override
                public void onUiSuccess(GroupPageInfo result) {
                    WebResponse<GroupPageInfo> response = WebResponse.createSuccess();
                    response.result = result;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取得 客服网址
     *
     * @param callback
     * @return
     */
    public void getService(SimpleCallback<String> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/customer/url";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            OKHttpHelper.get(url, params, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 取得影音域名
     *
     * @param callback
     * @return
     */
    public void getImagePathDomain(SimpleCallback<String> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/index/getImagePathDomain";
            Map<String, String> params = new HashMap<>();

            OKHttpHelper.get(url, params, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发现-聊天室列表
     */
    public void getChatRoomList(SimpleCallback<WebResponse<List<CustomChatRoomInfo>>> callback) {
        try {
            String url = APP_SERVER_ADDRESS + "/chatRoomList";
            Map<String, String> params = new HashMap<>();
            params.put("platform", "2");
            try {
                params.put("clientId", ChatManagerHolder.gChatManager.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            OKHttpHelper.get(url, params, new SimpleCallback<List<CustomChatRoomInfo>>() {
                @Override
                public void onUiSuccess(List<CustomChatRoomInfo> result) {
                    WebResponse<List<CustomChatRoomInfo>> response = WebResponse.createSuccess();
                    response.result = result;
                    callback.onSuccess(response);
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    callback.onUiFailure(code, msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 交易密码
     *
     * @param newPwd
     * @param doubleCheckPwd
     * @param oldPwd
     * @param callback
     */
    public void resetTradePassword(String newPwd, String doubleCheckPwd, String oldPwd, SimpleCallback<StatusResult> callback) {
        String url = APP_SERVER_ADDRESS + "/changeTradePassword";
        Map<String, Object> params = new HashMap<>();
        if (!TextUtils.isEmpty(oldPwd)) {
            params.put("oldPwd", Security.getSha256(oldPwd));
        }
        params.put("newPwd", Security.getSha256(newPwd));
        params.put("doubleCheckPwd", Security.getSha256(doubleCheckPwd));

        OKHttpHelper.post(url, params, callback);
    }

    /**
     * // 打印 Header log
     *
     * @param response
     */
    private void printRequestHeader(Response response) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("requestHeader:\n");
        Headers requestHeaders = response.networkResponse().request().headers();
        int requestHeadersLength = requestHeaders.size();
        for (int i = 0; i < requestHeadersLength; i++) {
            String headerName = requestHeaders.name(i);
            String headerValue = requestHeaders.get(headerName);
            stringBuffer.append(headerName).append(":").append(headerValue).append("\n");
        }

        LogHelper.e(TAG, stringBuffer.toString());
        stringBuffer.delete(0, stringBuffer.length());
    }

    public String hex_to_binary(String hex_input) {
        int decimal_int = Integer.parseInt(hex_input, 16);
        return Integer.toBinaryString(decimal_int);
    }
}
