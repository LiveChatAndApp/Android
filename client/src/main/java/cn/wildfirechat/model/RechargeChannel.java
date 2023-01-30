package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class RechargeChannel implements Parcelable {
    public int id;
    public String name;
    public int paymentMethod;
    public Info info;
    public int status;
    public String memo;
    public String createTime;
    public String updateTime;

    protected RechargeChannel(Parcel in) {
        id = in.readInt();
        name = in.readString();
        paymentMethod = in.readInt();
        info = in.readParcelable(Info.class.getClassLoader());
        status = in.readInt();
        memo = in.readString();
        createTime = in.readString();
        updateTime = in.readString();
    }

    public static final Creator<RechargeChannel> CREATOR = new Creator<RechargeChannel>() {
        @Override
        public RechargeChannel createFromParcel(Parcel in) {
            return new RechargeChannel(in);
        }

        @Override
        public RechargeChannel[] newArray(int size) {
            return new RechargeChannel[size];
        }
    };

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPaymentMethod(int paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getPaymentMethod() {
        return paymentMethod;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public Info getInfo() {
        return info;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getMemo() {
        return memo;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(paymentMethod);
        dest.writeParcelable(info, flags);
        dest.writeInt(status);
        dest.writeString(memo);
        dest.writeString(createTime);
        dest.writeString(updateTime);
    }


    public static class Info implements Parcelable {
        public String realName;
        public String bankName;
        public String bankAccount;
        public String qrCodeImage;

        public Info() {

        }

        protected Info(Parcel in) {
            realName = in.readString();
            bankName = in.readString();
            bankAccount = in.readString();
            qrCodeImage = in.readString();
        }

        public static final Parcelable.Creator<Info> CREATOR = new Parcelable.Creator<Info>() {
            @Override
            public Info createFromParcel(Parcel in) {
                return new Info(in);
            }

            @Override
            public Info[] newArray(int size) {
                return new Info[size];
            }
        };

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getRealName() {
            return realName;
        }

        public void setBankName(String bankName) {
            this.bankName = bankName;
        }

        public String getBankName() {
            return bankName;
        }

        public void setBankAccount(String bankAccount) {
            this.bankAccount = bankAccount;
        }

        public String getBankAccount() {
            return bankAccount;
        }

        public void setQrCodeImage(String qrCodeImage) {
            this.qrCodeImage = qrCodeImage;
        }

        public String getQrCodeImage() {
            return qrCodeImage;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(realName);
            dest.writeString(bankName);
            dest.writeString(bankAccount);
            dest.writeString(qrCodeImage);
        }
    }
}
