package cn.wildfire.chat.kit.recharge;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.List;

import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.model.RechargeChannel;
import cn.wildfirechat.model.RechargeResultInfo;
import cn.wildfirechat.model.WebResponse;

public class RechargeViewModel extends ViewModel {

    public LiveData<WebResponse<List<RechargeChannel>>> getWalletOrderDetail(String type) {
        MutableLiveData<WebResponse<List<RechargeChannel>>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().getRechargeChannel(type, new SimpleCallback<List<RechargeChannel>>() {
            @Override
            public void onUiSuccess(List<RechargeChannel> rechargeChannels) {
                WebResponse<List<RechargeChannel>> response = new WebResponse<>();
                response.code = 0;
                response.message = "success";
                response.result = rechargeChannels;
                liveData.postValue(response);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WebResponse<List<RechargeChannel>> response = new WebResponse<>();
                response.code = code;
                response.message = msg;
                liveData.postValue(response);
            }
        });
        return liveData;
    }

    public LiveData<WebResponse<RechargeResultInfo>> sendOrder(long amount, long channelId, String currency, int method) {
        MutableLiveData<WebResponse<RechargeResultInfo>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().sendRechargeOrder(amount, channelId, currency, method, new SimpleCallback<RechargeResultInfo>() {
            @Override
            public void onUiSuccess(RechargeResultInfo resultInfo) {
                WebResponse<RechargeResultInfo> response = new WebResponse<>();
                response.code = 0;
                response.message = "success";
                response.result = resultInfo;
                liveData.postValue(response);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                WebResponse<RechargeResultInfo> response = new WebResponse<>();
                response.code = code;
                response.message = msg;
                liveData.postValue(response);
            }
        });
        return liveData;
    }


    public LiveData<WebResponse<String>> uploadRechargeScreenshot(int id, File file) {
        MutableLiveData<WebResponse<String>> liveData = new MutableLiveData<>();
        ImplementUserSource.Instance().uploadRechargeScreenshot(id, file, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String resultInfo) {
                WebResponse<String> response = new WebResponse<>();
                response.code = 0;
                response.message = "success";
                response.result = resultInfo;
                liveData.postValue(response);
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

}
