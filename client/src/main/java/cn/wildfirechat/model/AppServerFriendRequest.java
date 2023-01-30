/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class AppServerFriendRequest implements Parcelable {

    public String uid;
    public String nickName;
    public String memberName;
    public String avatar;
    public String mobile;
    public String helloText;
    public boolean verify;
    public int gender;

    public AppServerFriendRequest() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.nickName);
        dest.writeString(this.memberName);
        dest.writeString(this.avatar);
        dest.writeString(this.mobile);
        dest.writeString(this.helloText);
        dest.writeInt(this.verify ? 1 : 0);
        dest.writeInt(this.gender);
    }

    protected AppServerFriendRequest(Parcel in) {
        this.uid = in.readString();
        this.nickName = in.readString();
        this.memberName = in.readString();
        this.avatar = in.readString();
        this.mobile = in.readString();
        this.helloText = in.readString();
        this.verify = in.readInt() == 1;
        this.gender = in.readInt();
    }

    public static final Creator<AppServerFriendRequest> CREATOR = new Creator<AppServerFriendRequest>() {
        @Override
        public AppServerFriendRequest createFromParcel(Parcel source) {
            return new AppServerFriendRequest(source);
        }

        @Override
        public AppServerFriendRequest[] newArray(int size) {
            return new AppServerFriendRequest[size];
        }
    };
}
