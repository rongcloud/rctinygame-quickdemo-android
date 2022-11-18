package cn.rongcloud.tinygame.oklib;

import java.util.HashMap;

/**
 * @author gyn
 * @date 2021/9/28
 */
public class OkParams {

    private HashMap<String, Object> mParams;

    public OkParams() {
        mParams = new HashMap<>();
    }

    public static OkParams get(){
        return new OkParams();
    }

    public OkParams add(String key, Object obj) {
        mParams.put(key, obj);
        return this;
    }

    public HashMap<String, Object> build() {
        return mParams;
    }
}
