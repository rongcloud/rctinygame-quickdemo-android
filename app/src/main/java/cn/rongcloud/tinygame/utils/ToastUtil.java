package cn.rongcloud.tinygame.utils;

import android.widget.Toast;


public class ToastUtil {
    public static void show(String msg) {
        Toast.makeText(UIKit.getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}