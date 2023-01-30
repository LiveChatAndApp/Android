package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CustomChatRoomInfo implements Parcelable {
    public int id;
    public String cid;
    public String name;
    public String image;
    public int status;
    public long createTime;
    public long updateTime;
    public int chatStatus;
    public String desc;
    public String extra;

    public CustomChatRoomInfo() {
    }

    protected CustomChatRoomInfo(Parcel in) {
        id = in.readInt();
        cid = in.readString();
        name = in.readString();
        image = in.readString();
        status = in.readInt();
        createTime = in.readLong();
        updateTime = in.readLong();
        chatStatus = in.readInt();
        desc = in.readString();
        extra = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(cid);
        dest.writeString(name);
        dest.writeString(image);
        dest.writeInt(status);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
        dest.writeInt(chatStatus);
        dest.writeString(desc);
        dest.writeString(extra);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CustomChatRoomInfo> CREATOR = new Creator<CustomChatRoomInfo>() {
        @Override
        public CustomChatRoomInfo createFromParcel(Parcel in) {
            return new CustomChatRoomInfo(in);
        }

        @Override
        public CustomChatRoomInfo[] newArray(int size) {
            return new CustomChatRoomInfo[size];
        }
    };
}
