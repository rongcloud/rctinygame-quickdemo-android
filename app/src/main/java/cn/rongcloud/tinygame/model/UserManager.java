package cn.rongcloud.tinygame.model;

import android.text.TextUtils;

import cn.rongcloud.tinygame.utils.GsonUtil;
import cn.rongcloud.tinygame.utils.SharedPreferUtil;

/**
 * @author gyn
 * @date 2022/3/4
 */
public class UserManager {

    private static final String USER = "USER";

    private static User mUser = null;

    public static void setUser(User user) {
        mUser = user;
        SharedPreferUtil.set(USER, GsonUtil.obj2Json(user));
    }

    public static User getUser() {
        if (mUser == null) {
            String json = SharedPreferUtil.get(USER);
            if (!TextUtils.isEmpty(json)) {
                mUser = GsonUtil.json2Obj(json, User.class);
            }
        }
        return mUser;
    }

    public static String getUserId() {
        return getUser().getUserId();
    }
}
