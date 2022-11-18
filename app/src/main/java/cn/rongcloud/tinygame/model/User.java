package cn.rongcloud.tinygame.model;

import android.text.TextUtils;

import java.io.Serializable;

import cn.rongcloud.tinygame.GameConstant;

public class User implements Serializable {
    String userId;
    String userName;
    String portrait;
    int type;
    String authorization;
    String imToken;

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPortrait() {
        return TextUtils.isEmpty(portrait) ?
                GameConstant.DEFAULT_PORTRAIT
                : GameConstant.FILE_URL + portrait;
    }

    public int getType() {
        return type;
    }

    public String getAuthorization() {
        return authorization;
    }

    public String getImToken() {
        return imToken;
    }
}