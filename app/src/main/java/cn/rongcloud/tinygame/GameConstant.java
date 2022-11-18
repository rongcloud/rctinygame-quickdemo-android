package cn.rongcloud.tinygame;

/**
 * @author gyn
 * @date 2022/3/4
 */
public class GameConstant {
    // 服务器地址
    private static final String HOST = BuildConfig.BASE_SERVER_ADDRES;
    // 登录游戏获取appCode的接口
    public static final String GAME_LOGIN_URL = HOST + "mic/game/login";
    // 登录账号获取用户信息的接口
    public static final String LOGIN_URL = HOST + "user/login";
    // 默认头像地址
    public static final String DEFAULT_PORTRAIT = "https://cdn.ronghub.com/demo/default/rce_default_avatar.png";
    // 用户头像前缀
    public static final String FILE_URL = HOST + "file/show?path=";
}
