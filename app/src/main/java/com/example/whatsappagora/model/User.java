package com.example.whatsappagora.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {
    private String fireUid, fireDisplayName;
    private int agoraUid;

    public User(String fireUid) {
        setFireUid(fireUid);
    }

    protected User(Parcel in) {
        fireUid = in.readString();
        fireDisplayName = in.readString();
        agoraUid = in.readInt();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getFireUid() {
        return fireUid;
    }

    public void setFireUid(String fireUid) {
        this.fireUid = fireUid;
    }

    public String getFireDisplayName() {
        return fireDisplayName;
    }

    public void setFireDisplayName(String fireDisplayName) {
        this.fireDisplayName = fireDisplayName;
    }

    public int getAgoraUid() {
        return agoraUid;
    }

    public void setAgoraUid(int agoraUid) {
        this.agoraUid = agoraUid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(fireUid);
        dest.writeString(fireDisplayName);
        dest.writeInt(agoraUid);
    }
}
