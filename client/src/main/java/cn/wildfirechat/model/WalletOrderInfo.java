package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 我的钱包-订单明細
 */
public class WalletOrderInfo implements Parcelable {
    public int id;
    public int type;
    public RechargeChannel rechargeChannel;
    public String orderCode;
    public String amount;
    public int status;
    public long createTime;

    protected WalletOrderInfo(Parcel in) {
        id = in.readInt();
        type = in.readInt();
        rechargeChannel = in.readParcelable(RechargeChannel.class.getClassLoader());
        orderCode = in.readString();
        amount = in.readString();
        status = in.readInt();
        createTime = in.readLong();
    }

    public static final Creator<WalletOrderInfo> CREATOR = new Creator<WalletOrderInfo>() {
        @Override
        public WalletOrderInfo createFromParcel(Parcel in) {
            return new WalletOrderInfo(in);
        }

        @Override
        public WalletOrderInfo[] newArray(int size) {
            return new WalletOrderInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(type);
        dest.writeParcelable(rechargeChannel, flags);
        dest.writeString(orderCode);
        dest.writeString(amount);
        dest.writeInt(status);
        dest.writeLong(createTime);
    }
}
