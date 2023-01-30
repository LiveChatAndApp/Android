package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

// 我的錢包
// 提現
// 收款方式列表
// 添加提币收款方式
// 移除提币收款方式
public class WalletInfo implements Parcelable {
    /*
    "currency":"CNY",
    "balance":"20.00",
    "freeze":"1.00",
    "canRecharge":true,
    "canWithdraw":true
     */

    public String currency;
    public String balance;
    public String freeze;
    public boolean canRecharge;
    public boolean canWithdraw;
    public boolean error = false;

    public WalletInfo() {
    }

    protected WalletInfo(Parcel in) {
        this.currency = in.readString();
        this.balance = in.readString();
        this.freeze = in.readString();
        this.canRecharge = in.readInt() == 1;
        this.canWithdraw = in.readInt() == 1;
        this.error = in.readInt() == 1;
    }

    public static final Creator<WalletInfo> CREATOR = new Creator<WalletInfo>() {
        @Override
        public WalletInfo createFromParcel(Parcel in) {
            return new WalletInfo(in);
        }

        @Override
        public WalletInfo[] newArray(int size) {
            return new WalletInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(currency);
        dest.writeString(balance);
        dest.writeString(freeze);
        dest.writeInt(canRecharge ? 1 : 0);
        dest.writeInt(canWithdraw ? 1 : 0);
        dest.writeInt(error ? 1 : 0);
    }
}
