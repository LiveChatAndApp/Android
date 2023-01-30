/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by heavyrainlee on 17/12/2017.
 */
public class GroupPageInfo implements Parcelable {
    public List<Item> data;
    public int page;
    public int pageSize;
    public int totalPage;
    public int totalElement;

    public GroupPageInfo() {

    }

    public GroupPageInfo(Parcel in) {
        page = in.readInt();
        pageSize = in.readInt();
        totalPage = in.readInt();
        totalElement = in.readInt();
    }

    public static final Creator<GroupPageInfo> CREATOR = new Creator<GroupPageInfo>() {
        @Override
        public GroupPageInfo createFromParcel(Parcel in) {
            return new GroupPageInfo(in);
        }

        @Override
        public GroupPageInfo[] newArray(int size) {
            return new GroupPageInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GroupPageInfo) {
            return ((GroupPageInfo) obj).page == page && ((GroupPageInfo) obj).totalPage == totalPage;
        } else {
            return false;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(page);
        dest.writeInt(pageSize);
        dest.writeInt(totalPage);
        dest.writeInt(totalElement);
    }

    public static class Item implements Parcelable {
        public String gid;
        public String groupName;
        public String portrait;

        public Item() {

        }

        public Item(Parcel in) {
            gid = in.readString();
            groupName = in.readString();
            portrait = in.readString();
        }

        public static final Creator<Item> CREATOR = new Creator<Item>() {
            @Override
            public Item createFromParcel(Parcel in) {
                return new Item(in);
            }

            @Override
            public Item[] newArray(int size) {
                return new Item[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(gid);
            dest.writeString(groupName);
            dest.writeString(portrait);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Item) {
                return ((Item) obj).gid.equals(gid) && ((Item) obj).groupName.equals(groupName);
            } else {
                return false;
            }
        }
    }
}
