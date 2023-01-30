package cn.wildfire.chat.kit.qrcode;

import androidx.lifecycle.ViewModel;

import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.ResultWrapper;

public class QrCodeViewModel extends ViewModel {

    public void getQrCode(SimpleCallback<GetStringResult> callback) {
        ImplementUserSource.Instance().qrcode(callback);
    }

}
