package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PaymentInfo implements Parcelable {
    /*
{
"id":5,
"userId":95,
"paymentMethod":1,
"info":"{\"channel\":\"WDBC\",\"info\":\"Lance Lai\",\"name\":\"myCard\"}",
"image":null,
"createTime":"2022-11-07T06:01:53.000+00:00",
"updateTime":"2022-11-07T06:01:53.000+00:00"
}
     */

    public int id;
    public int userId;
    public int paymentMethod;
    /**
     * bank name
     */
    public String bankName;
    public String info;
    /**
     * owner name
     */
    public String ownerName;
    public String bankCardNumber;
    /**
     * custom payment Name
     */
    public String name;
    public String image;
    public String createTime;
    public String updateTime;
    public boolean error;

    public PaymentInfo() {
    }

    protected PaymentInfo(Parcel in) {
        this.id = in.readInt();
        this.userId = in.readInt();
        this.paymentMethod = in.readInt();
        this.bankName = in.readString();
        this.info = in.readString();
        this.ownerName = in.readString();
        this.bankCardNumber = in.readString();
        this.name = in.readString();
        this.image = in.readString();
        this.createTime = in.readString();
        this.updateTime = in.readString();
    }

    public static final Creator<PaymentInfo> CREATOR = new Creator<PaymentInfo>() {
        @Override
        public PaymentInfo createFromParcel(Parcel in) {
            return new PaymentInfo(in);
        }

        @Override
        public PaymentInfo[] newArray(int size) {
            return new PaymentInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(userId);
        dest.writeInt(paymentMethod);
        dest.writeString(bankName);
        dest.writeString(info);
        dest.writeString(ownerName);
        dest.writeString(name);
        dest.writeString(bankCardNumber);
        dest.writeString(image);
        dest.writeString(createTime);
        dest.writeString(updateTime);
    }
}
