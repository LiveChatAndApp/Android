package cn.wildfire.chat.kit.user.password;

import androidx.lifecycle.ViewModel;

import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.net.base.StatusResult;

public class TradePasswordViewModel extends ViewModel {

    public void resetTradePassword(String newPwd, String doubleCheckPwd, String oldPwd, SimpleCallback<StatusResult> callback) {
        ImplementUserSource.Instance().resetTradePassword(newPwd, doubleCheckPwd, oldPwd, callback);
    }
}
