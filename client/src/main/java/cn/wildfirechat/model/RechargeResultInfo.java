package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class RechargeResultInfo implements Parcelable {
    private int id;
    private String orderCode;
    private int userId;
    private int method;
    private long amount;
    private int channelId;
    private String payImage;
    private String currency;
    private int status;
    private String createTime;
    private String completeTime;
    private String updaterId;
    private int updaterRole;
    private String updateTime;

    public RechargeResultInfo() {
    }

    protected RechargeResultInfo(Parcel in) {
        id = in.readInt();
        orderCode = in.readString();
        userId = in.readInt();
        method = in.readInt();
        amount = in.readLong();
        channelId = in.readInt();
        payImage = in.readString();
        currency = in.readString();
        status = in.readInt();
        createTime = in.readString();
        completeTime = in.readString();
        updaterId = in.readString();
        updaterRole = in.readInt();
        updateTime = in.readString();
    }

    public static final Creator<RechargeResultInfo> CREATOR = new Creator<RechargeResultInfo>() {
        @Override
        public RechargeResultInfo createFromParcel(Parcel in) {
            return new RechargeResultInfo(in);
        }

        @Override
        public RechargeResultInfo[] newArray(int size) {
            return new RechargeResultInfo[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getPayImage() {
        return payImage;
    }

    public void setPayImage(String payImage) {
        this.payImage = payImage;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(String completeTime) {
        this.completeTime = completeTime;
    }

    public String getUpdaterId() {
        return updaterId;
    }

    public void setUpdaterId(String updaterId) {
        this.updaterId = updaterId;
    }

    public int getUpdaterRole() {
        return updaterRole;
    }

    public void setUpdaterRole(int updaterRole) {
        this.updaterRole = updaterRole;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(orderCode);
        dest.writeInt(userId);
        dest.writeInt(method);
        dest.writeLong(amount);
        dest.writeInt(channelId);
        dest.writeString(payImage);
        dest.writeString(currency);
        dest.writeInt(status);
        dest.writeString(createTime);
        dest.writeString(completeTime);
        dest.writeString(updaterId);
        dest.writeInt(updaterRole);
        dest.writeString(updateTime);
    }
}
