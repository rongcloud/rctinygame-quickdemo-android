package cn.rongcloud.tinygame.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

import cn.rongcloud.gamelib.api.RCGameEngine;
import cn.rongcloud.gamelib.api.interfaces.RCDrawGuessListener;
import cn.rongcloud.gamelib.api.interfaces.RCGamePlayerStateListener;
import cn.rongcloud.gamelib.api.interfaces.RCGameStateListener;
import cn.rongcloud.gamelib.callback.RCGameListCallback;
import cn.rongcloud.gamelib.error.RCGameError;
import cn.rongcloud.gamelib.model.RCGameInfo;
import cn.rongcloud.gamelib.model.RCGameLoadingStage;
import cn.rongcloud.gamelib.model.RCGameOption;
import cn.rongcloud.gamelib.model.RCGameRoomInfo;
import cn.rongcloud.gamelib.model.RCGameSafeRect;
import cn.rongcloud.gamelib.model.RCGameSettle;
import cn.rongcloud.gamelib.model.RCGameSound;
import cn.rongcloud.gamelib.model.RCGameState;
import cn.rongcloud.gamelib.model.RCGameUI;
import cn.rongcloud.gamelib.utils.VMLog;
import cn.rongcloud.rtc.api.RCRTCConfig;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.callback.IRCRTCAudioDataListener;
import cn.rongcloud.rtc.base.RCRTCAudioFrame;
import cn.rongcloud.rtc.base.RCRTCParamsType;
import cn.rongcloud.tinygame.GameConstant;
import cn.rongcloud.tinygame.R;
import cn.rongcloud.tinygame.model.LoginBean;
import cn.rongcloud.tinygame.model.SeatPlayer;
import cn.rongcloud.tinygame.model.UserManager;
import cn.rongcloud.tinygame.oklib.OkApi;
import cn.rongcloud.tinygame.oklib.OkParams;
import cn.rongcloud.tinygame.oklib.WrapperCallBack;
import cn.rongcloud.tinygame.oklib.wrapper.Wrapper;
import cn.rongcloud.tinygame.utils.ToastUtil;
import cn.rongcloud.tinygame.utils.UIKit;
import cn.rongcloud.tinygame.widget.loading.LoadTag;
import cn.rongcloud.voiceroom.api.RCVoiceRoomEngine;
import cn.rongcloud.voiceroom.api.callback.RCVoiceRoomCallback;
import cn.rongcloud.voiceroom.api.callback.RCVoiceRoomEventListener;
import cn.rongcloud.voiceroom.model.RCPKInfo;
import cn.rongcloud.voiceroom.model.RCVoiceRoomInfo;
import cn.rongcloud.voiceroom.model.RCVoiceSeatInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.TextMessage;

/**
 * @author gyn
 * @date 2022/3/3
 */
public class GameActivity extends AppCompatActivity implements RCGameStateListener, RCGamePlayerStateListener, RCDrawGuessListener, GameSeatAdapter.OnSeatClickListener {
    private static final String TAG = GameActivity.class.getSimpleName();
    private static final String GAME_INFO = "GAME_INFO";
    private static final String IS_CREATE = "IS_CREATE";
    private static final String ROOM_ID = "ROOM_ID";
    // 固定9个麦位，根据业务需求设置
    private static final int SEAT_COUNT = 9;
    // 装载游戏的view
    private FrameLayout flContainer;

    // 游戏背景
    private ImageView ivBg;
    // 切换游戏按钮
    private ImageView ivSwitch;
    // 退出按钮
    private ImageView ivClose;
    // 底部输入框
    private EditText etInput;
    // 游戏座位适配器
    private GameSeatAdapter gameSeatAdapter;
    // 游戏座位
    private RecyclerView rvSeat;
    // 消息区域
    private RecyclerView rvMessage;
    // 消息适配器
    private MessageAdapter messageAdapter;
    // 麦克风图标
    private ImageView ivMic;

    private TextView tvTitle;

    // 登录游戏后获取的appcode
    private String mAppCode;
    // 用户id
    private String mUserId = UserManager.getUserId();
    // 游戏信息
    private RCGameInfo mGameInfo;
    // 是否是创建
    private boolean isCreate;
    // 房间id
    private String mRoomId;
    // 你画我猜返回的关键词
    private String mKeyword;
    // 当前游戏的状态
    private RCGameState mGameState = RCGameState.IDLE;
    // 游戏的队长
    private String mCaptainUserId = "";
    // 玩家是否打开了mic
    private boolean isOpenMic = false;

    private LoadTag mLoadingView;

    public static void launch(Context context, RCGameInfo gameInfo, String roomId, boolean isCreate) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(GAME_INFO, gameInfo);
        intent.putExtra(IS_CREATE, isCreate);
        intent.putExtra(ROOM_ID, roomId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Serializable serializable = getIntent().getSerializableExtra(GAME_INFO);
        if (serializable != null) {
            mGameInfo = (RCGameInfo) serializable;
        }
        isCreate = getIntent().getBooleanExtra(IS_CREATE, true);
        mRoomId = getIntent().getStringExtra(ROOM_ID);
        initView();
        // 创建或加入语聊房->加载游戏
        createOrJoinRoom();
    }

    // 初始化view
    private void initView() {
        mLoadingView = new LoadTag(this);
        flContainer = findViewById(R.id.fl_container);
        ivBg = (ImageView) findViewById(R.id.iv_bg);
        ivSwitch = (ImageView) findViewById(R.id.iv_switch);
        ivClose = (ImageView) findViewById(R.id.iv_close);
        rvSeat = (RecyclerView) findViewById(R.id.rv_seat);
        etInput = (EditText) findViewById(R.id.et_input);
        rvMessage = findViewById(R.id.rv_message);
        tvTitle = findViewById(R.id.tv_title);
        ivMic = findViewById(R.id.iv_mic);
        ivMic.setSelected(isOpenMic);

        // 加载游戏背景
        Glide.with(this).load(mGameInfo.getLoadingPic()).into(ivBg);
        // 关闭按钮
        ivClose.setOnClickListener(v -> onBackPressed());
        // 切换游戏按钮
        ivSwitch.setOnClickListener(v -> {
            switchGame();
        });
        // 座位设置
        gameSeatAdapter = new GameSeatAdapter(this, SEAT_COUNT, this::onClickSeat);
        rvSeat.setAdapter(gameSeatAdapter);
        // 消息区域
        messageAdapter = new MessageAdapter(this);
        rvMessage.setAdapter(messageAdapter);

        tvTitle.setText("房间id：" + mRoomId + "    用户名：" + UserManager.getUser().getUserName());
    }

    // 队长切换游戏
    private void switchGame() {
        if (mGameState != RCGameState.IDLE) {
            ToastUtil.show("空闲状态才可以切换游戏");
            return;
        }
        RCGameEngine.getInstance().getGameList(new RCGameListCallback() {
            @Override
            public void onSuccess(List<RCGameInfo> gameInfoList) {
                String[] games = new String[gameInfoList.size()];
                for (int i = 0; i < gameInfoList.size(); i++) {
                    games[i] = gameInfoList.get(i).getGameName();
                }
                new AlertDialog.Builder(GameActivity.this)
                        .setItems(games, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mGameInfo = gameInfoList.get(which);
                                Glide.with(GameActivity.this).load(mGameInfo.getLoadingPic()).into(ivBg);
                                RCGameEngine.getInstance().switchGame(mGameInfo.getGameId());
                                gameSeatAdapter.deleteCaptain();
                                ivSwitch.setVisibility(View.GONE);
                                // 队长切换游戏后，可发送自定义消息给其他玩家,其他玩家也要切换游戏，这里为了简单就发一条文字消息做通知
                                MessageContent messageContent = new TextMessage("gameId:" + mGameInfo.getGameId());
                                RCVoiceRoomEngine.getInstance().sendRoomMessage(messageContent, null);
                                dialog.dismiss();
                            }
                        })
                        .setTitle("切换游戏")
                        .show();
            }

            @Override
            public void onError(int code, RCGameError error) {

            }
        });

    }

    // 创建或者加入语聊房
    private void createOrJoinRoom() {
        // 设置语聊房监听，参见语聊房QuickDemo具体使用，这里只是简单的实现
        RCVoiceRoomEngine.getInstance().setVoiceRoomEventListener(roomEventListener);

        // 这里为了支持游戏内语音识别，需设置固定采样率16K，单声道
        // 如果想采用高采样率，可以自己在数据回调中对数据重采样为16K单声道
        // RCGameEngine.getInstance().pushAudio(); 这个方法只支持16K单声道的PCM数据
        RCRTCConfig rcrtcConfig = RCRTCConfig.Builder.create()
                // 设置16K采样率
                .setAudioSampleRate(16000)
                // 关闭立体声
                .enableStereo(false)
                .build();

        if (isCreate) {
            // 构建房间信息
            RCVoiceRoomInfo roomInfo = new RCVoiceRoomInfo();
            // 最大座位数
            roomInfo.setSeatCount(SEAT_COUNT);
            // 房间名称，暂用房间id
            roomInfo.setRoomName(mRoomId);
            // 可以自由上麦
            roomInfo.setFreeEnterSeat(true);

            RCVoiceRoomEngine.getInstance().createAndJoinRoom(rcrtcConfig, mRoomId, roomInfo, new RCVoiceRoomCallback() {
                @Override
                public void onSuccess() {
                    initAndLoadGame();
                    VMLog.d(TAG, "==============创建房间成功");
                }

                @Override
                public void onError(int i, String s) {
                    VMLog.e(TAG, "==============创建房间失败 code：" + i + " msg: " + s);
                }
            });
        } else {
            RCVoiceRoomEngine.getInstance().joinRoom(rcrtcConfig, mRoomId, new RCVoiceRoomCallback() {
                @Override
                public void onSuccess() {
                    // 设置麦克风状态
                    RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
                    initAndLoadGame();
                    VMLog.d(TAG, "==============加入房间成功");
                }

                @Override
                public void onError(int i, String s) {
                    VMLog.e(TAG, "==============加入房间失败 code：" + i + " msg: " + s);
                }
            });
        }
    }


    // 登录游戏服务器->加载游戏
    private void initAndLoadGame() {
        // 请求开发者服务器登录用户，获取appCode
        login(new LoginCallback() {
            @Override
            public void onSuccess(String code) {
                mAppCode = code;
                // 加载游戏
                loadGame();
            }

            @Override
            public void onError() {

            }
        });
    }


    /**
     * 登录开发者服务器获取code
     *
     * @param callback
     */
    private void login(LoginCallback callback) {
        OkApi.post(GameConstant.GAME_LOGIN_URL, OkParams.get().add("userId", mUserId).build(), new WrapperCallBack() {

            @Override
            public void onResult(Wrapper result) {
                Log.e("==========", result.getBody().toString());
                if (result.ok()) {
                    LoginBean loginBean = result.get(LoginBean.class);
                    if (loginBean != null) {
                        callback.onSuccess(loginBean.code);
                    } else {
                        callback.onError();
                    }
                } else {
                    callback.onError();
                }
            }
        });
    }

    // 加载游戏
    private void loadGame() {
        // 注册监听,在init成功后loadGame之前注册监听
        RCGameEngine.getInstance().setGameStateListener(this);
        RCGameEngine.getInstance().setGamePlayerStateListener(this);
        RCGameEngine.getInstance().setDrawGuessListener(this);
        // 配置游戏
        RCGameOption option = RCGameOption.builder()
                // 游戏内可操作按钮的四周边距，单位dp
                .setGameSafeRect(new RCGameSafeRect(10, 100, 10, 200))// 空出顶部麦位和底部消息区域
                // 设置游戏内音乐开关和音量
                .setGameSound(new RCGameSound(RCGameSound.SoundControl.OPEN, 100))
                // 设置游戏内UI布局的展示
                .setGameUI(RCGameUI.builder()
                        // 设置游戏内麦位隐藏
                        .setLobbyPlayersHide(false)
                        // 隐藏版本号
                        .setVersionHide(true)
                        .build())
                .build();
        RCGameRoomInfo gameRoomInfo = new RCGameRoomInfo();
        gameRoomInfo.setGameId(mGameInfo.getGameId());
        gameRoomInfo.setRoomId(mRoomId);
        gameRoomInfo.setUserId(mUserId);
        gameRoomInfo.setAppCode(mAppCode);
        // 加载游戏
        RCGameEngine.getInstance().loadGame(this, flContainer, gameRoomInfo, option);
    }

    /**
     * 点击发送按钮
     *
     * @param view
     */
    public void clickSend(View view) {
        String text = etInput.getText().toString();
        if (!TextUtils.isEmpty(text)) {
            // 添加消息到自己公屏
            addMessage(UserManager.getUser().getUserName() + "：" + text);
            // 清空输入框
            etInput.setText("");
            // 发送消息到语聊房
            MessageContent messageContent = TextMessage.obtain(text);
            messageContent.setUserInfo(new UserInfo(UserManager.getUserId(), UserManager.getUser().getUserName(), Uri.parse(UserManager.getUser().getPortrait())));
            RCVoiceRoomEngine.getInstance().sendRoomMessage(messageContent, null);

            // 你画我猜中，发送的消息是关键词就调用命中接口
            if (TextUtils.equals(mKeyword, text)) {
                RCGameEngine.getInstance().hitKeyword(text, null);
            }
            // 数字炸弹游戏，发送的数字
            if (TextUtils.equals(mGameInfo.getGameId(), "1468091457989509190")) {
                RCGameEngine.getInstance().hitKeyword(text, null);
            }
        }
    }

    // 点击麦克风图标
    public void clickMic(View view) {
        isOpenMic = !isOpenMic;
        RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
        ivMic.setSelected(isOpenMic);
    }

    /**
     * 消息添加到公屏并且移动到最后位置
     *
     * @param message
     */
    private void addMessage(String message) {
        messageAdapter.addMessage(message);
        rvMessage.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
    }

    // 自己上麦或者下麦
    private void handleEnterSeat(int seatIndex) {
        mLoadingView.show("上麦中");
        // 更改麦克风状态
        RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
        // 上麦
        RCVoiceRoomEngine.getInstance().enterSeat(seatIndex, new RCVoiceRoomCallback() {
            @Override
            public void onSuccess() {
                ToastUtil.show("上麦成功");
                // 语聊房SDK内部AudioScenario采用的MUSIC_CHATROOM，这里改成DEFAULT可以屏蔽掉游戏的背景音乐被播放出去
                RCRTCEngine.getInstance().getDefaultAudioStream().setAudioQuality(RCRTCParamsType.AudioQuality.MUSIC_HIGH, RCRTCParamsType.AudioScenario.DEFAULT);
                VMLog.d(TAG, "==============enterSeat success");
                mLoadingView.dismiss();
            }

            @Override
            public void onError(int i, String s) {
                ToastUtil.show("上麦失败");
                VMLog.e(TAG, "==============enterSeat 失败 code：" + i + " msg: " + s);
                mLoadingView.dismiss();
            }
        });
    }

    private void handleLeaveSeat() {
        RCGameEngine.getInstance().cancelReadyGame(null);
        RCGameEngine.getInstance().cancelJoinGame(null);
        RCVoiceRoomEngine.getInstance().leaveSeat(new RCVoiceRoomCallback() {
            @Override
            public void onSuccess() {
                VMLog.d(TAG, "==============leaveSeat success");
                RCGameEngine.getInstance().cancelJoinGame(null);
            }

            @Override
            public void onError(int i, String s) {
                VMLog.e(TAG, "==============leaveSeat失败 code：" + i + " msg: " + s);
            }
        });
    }

    /**
     * 点击麦位
     *
     * @param position
     * @param seatPlayer
     */
    @Override
    public void onClickSeat(int position, SeatPlayer seatPlayer) {
        // 座位上有人
        if (seatPlayer != null) {
            // 自己是队长
            if (TextUtils.equals(mUserId, mCaptainUserId)) {
                // 队长点击自己且在游戏中，可以结束游戏
                if (TextUtils.equals(mUserId, seatPlayer.userId)) {
                    if (mGameState == RCGameState.PLAYING) {
                        new AlertDialog.Builder(this)
                                .setTitle("你确定要结束本局游戏吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 结束游戏
                                        RCGameEngine.getInstance().endGame(null);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {// 不在游戏中，点击后可下麦
                        new AlertDialog.Builder(this)
                                .setTitle("你确定要下麦吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 下麦并退出游戏
                                        handleLeaveSeat();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                } else {// 队长点击别人可以踢人
                    new AlertDialog.Builder(this)
                            .setTitle("请选择你要对该用户的操作？")
                            .setPositiveButton("踢出", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // 踢人
                                    RCVoiceRoomEngine.getInstance().kickUserFromSeat(seatPlayer.userId, null);
                                    RCGameEngine.getInstance().kickPlayer(seatPlayer.userId, null);
                                    dialog.dismiss();
                                }
                            })

                            .setNegativeButton("转移队长", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RCGameEngine.getInstance().setCaptain(seatPlayer.userId, null);
                                    dialog.dismiss();
                                }
                            }).show();
                }
            } else {
                // 队员点击自己且在游戏中,可以退出游戏不玩了
                if (TextUtils.equals(seatPlayer.userId, mUserId)) {
                    if (mGameState == RCGameState.PLAYING) {
                        new AlertDialog.Builder(this)
                                .setTitle("你确定要退出游戏吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 退出游戏
                                        RCGameEngine.getInstance().cancelPlayGame(null);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("你确定要下麦吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 下麦并退出游戏
                                        handleLeaveSeat();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                }
            }
        } else {
            boolean isInSeat = gameSeatAdapter.existSeat(mUserId);
            if (isInSeat) {
                ToastUtil.show("您已经在麦位上了");
                // RCVoiceRoomEngine.getInstance().switchSeatTo(position, new RCVoiceRoomCallback() {
                //     @Override
                //     public void onSuccess() {
                //         VMLog.d("切换麦位成功");
                //     }
                //
                //     @Override
                //     public void onError(int i, String s) {
                //         VMLog.d("切换麦位失败:code:" + i + " msg" + s);
                //     }
                // });
            } else {
                mLoadingView.show("上麦中");
                // 点击麦位上麦成功后加入游戏
                RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
                RCVoiceRoomEngine.getInstance().enterSeat(position, new RCVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.show("上麦成功");
                        // 语聊房SDK内部AudioScenario采用的MUSIC_CHATROOM，这里改成DEFAULT可以屏蔽掉游戏的背景音乐被播放出去
                        RCRTCEngine.getInstance().getDefaultAudioStream().setAudioQuality(RCRTCParamsType.AudioQuality.MUSIC_HIGH, RCRTCParamsType.AudioScenario.DEFAULT);

                        RCGameEngine.getInstance().joinGame(null);
                        mLoadingView.dismiss();
                    }

                    @Override
                    public void onError(int i, String s) {
                        ToastUtil.show("上麦失败:" + s);
                        mLoadingView.dismiss();
                    }
                });
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // RCGameEngine.getInstance().startEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // RCGameEngine.getInstance().resumeEngine();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // RCGameEngine.getInstance().pauseEngine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // RCGameEngine.getInstance().stopEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RCGameEngine.getInstance().destroyEngine();
    }

    @Override
    public void onBackPressed() {
        // 关闭页面前先调用离开房间，否则下次会加入失败
        RCVoiceRoomEngine.getInstance().leaveRoom(new RCVoiceRoomCallback() {
            @Override
            public void onSuccess() {
                GameActivity.super.onBackPressed();
            }

            @Override
            public void onError(int i, String s) {
                GameActivity.super.onBackPressed();
            }
        });
    }

    // ==================玩家状态监听回调================

    @Override
    public void onPlayerIn(String userId, boolean isIn, int teamId) {
        // 当有人加入/退出游戏
        VMLog.d(TAG, "onPlayerIn:" + userId + " " + isIn);
        int seatIndex = -1;
        if (isIn) {
            seatIndex = gameSeatAdapter.getEmptySeat(userId);
            gameSeatAdapter.updateSeat(userId, SeatPlayer.PlayerState.JOIN);
        } else {
            seatIndex = gameSeatAdapter.getSeatIndex(userId);
            gameSeatAdapter.updateSeat(userId, SeatPlayer.PlayerState.IDLE);
        }
        // 如果是自己的话，执行上下麦逻辑
        if (TextUtils.equals(userId, mUserId) && seatIndex != -1) {
            // 进游戏上麦
            if (isIn) {
                handleEnterSeat(seatIndex);
            } else {
                // 退游戏不下麦
            }
        }
        // 队长走了
        if (!isIn && TextUtils.equals(mCaptainUserId, userId)) {
            mCaptainUserId = "";
            refreshCaptain();
        }
    }

    @Override
    public void onPlayerCaptain(String userId, boolean isCaptain) {
        // 队长更换
        VMLog.d(TAG, "onPlayerCaptain:" + userId + " " + isCaptain);
        if (isCaptain) {
            mCaptainUserId = userId;
            refreshCaptain();
        }
    }

    private void refreshCaptain() {
        gameSeatAdapter.setCaptain(mCaptainUserId);
        // 如果是队长
        if (TextUtils.equals(mCaptainUserId, mUserId)) {
            ivSwitch.setVisibility(View.VISIBLE);
        } else {
            ivSwitch.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPlayerReady(String userId, boolean isReady) {
        // 玩家准备/取消准备
        VMLog.d(TAG, "onPlayerReady:" + userId + " " + isReady);
        gameSeatAdapter.updateSeat(userId, isReady ? SeatPlayer.PlayerState.READY : SeatPlayer.PlayerState.JOIN);
    }

    @Override
    public void onPlayerPlaying(String userId, boolean isPlaying) {
        // 玩家游戏状态改变
        VMLog.d(TAG, "onPlayerPlaying:" + userId + " " + isPlaying);
        gameSeatAdapter.setPlaying(userId, isPlaying);
    }

    @Override
    public void onPlayerChangeSeat(String userId, int from, int to) {
        // 座位改变
        VMLog.d(TAG, "onPlayerChangeSeat:" + userId + " " + from + "->" + to);
    }

    @Override
    public void onPlayerDieStatus(String userId, boolean isDeath) {
        VMLog.d(TAG, "onPlayerDieStatus:" + userId + " isDeath" + isDeath);
    }

    @Override
    public void onPlayerTurnStatus(String userId, boolean isTurn) {
        VMLog.d(TAG, "onPlayerDieStatus:" + userId + " isTurn" + isTurn);
    }

    @Override
    public void onGameLoadingProgress(RCGameLoadingStage loadingStage, int errorCode, int progress) {
        VMLog.d(TAG, loadingStage + " " + errorCode + " " + progress);
    }

    // ==================游戏状态监听回调================
    @Override
    public void onGameLoaded() {
        // 游戏加载成功
        VMLog.d(TAG, "onGameLoaded");
    }

    @Override
    public void onGameDestroyed() {
        // 游戏销毁
        VMLog.d(TAG, "onGameDestroyed");
        stopVoiceRecorder();
    }

    @Override
    public void onReceivePublicMessage(String message) {
        // 游戏内公屏通知
        VMLog.d(TAG, "onReceivePublicMessage:" + message);
        addMessage(message);
    }

    @Override
    public void onKeywordToHit(String keyword) {
        // 你画我猜游戏返回的关键词
        // 当别人选词后，选择的关键词会返回，自己持有
        // 当发消息时，看看是否是关键词，做命中操作
        mKeyword = keyword;
    }

    @Override
    public void onGameStateChanged(RCGameState gameState) {
        // 游戏状态改变
        // gameState=0 (idle 状态，游戏未开始，空闲状态）；
        // gameState=1 （loading 状态，所有玩家都准备好，队长点击了开始游戏按钮，等待加载游戏场景开始游戏）；
        // gameState=2（playing状态，游戏进行中状态）
        VMLog.d(TAG, "onGameStateChanged:" + gameState);
        mGameState = gameState;
        // TODO 游戏开始时可以收起/隐藏消息区域，避免影响游戏内操作，其他状态可以显示
        // 用户根据自己需求控制
        switch (gameState) {
            case IDLE:
                break;
            case LOADING:
                break;
            case PLAYING:
                break;
        }
    }

    @Override
    public void onMicrophoneChanged(boolean isOpen) {
        isOpenMic = isOpen;
        ivMic.setSelected(isOpenMic);
        RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpen);
    }

    /**
     * 游戏中语音输入开启
     *
     * @param isOpen 是否开启
     */
    @Override
    public void onGameASRChanged(boolean isOpen) {
        VMLog.d("语音识别开启了？可以推送数据了？ " + isOpen);
        // 根据自己业务需求处理，一般如果不闭麦自己语音猜的词别人都能听到，不符合游戏逻辑

        if (isOpen) {
            startVoiceRecorder();
        } else {
            stopVoiceRecorder();
        }
    }


    private void startVoiceRecorder() {
        // 设置数据流监听
        RCRTCEngine.getInstance().getDefaultAudioStream().setRecordAudioDataListener(dataListener);
    }

    private void stopVoiceRecorder() {
        // 移除数据流监听
        if (RCRTCEngine.getInstance().getDefaultAudioStream() != null) {
            RCRTCEngine.getInstance().getDefaultAudioStream().setRecordAudioDataListener(null);
        }
    }

    private IRCRTCAudioDataListener dataListener = new IRCRTCAudioDataListener() {
        // 这里每10ms回调一次，如果想一次性发送更多数据，可自己合并数据再调用pushAudio
        @Override
        public byte[] onAudioFrame(RCRTCAudioFrame rcRTCAudioFrame) {
            // 如果没设置16K采样率单声道，可以在这里拦截数据做重采样
            ByteBuffer input = ByteBuffer.wrap(rcRTCAudioFrame.getBytes());
            UIKit.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /**
                     * 传入的音频切片数据必须是：PCM格式，采样率：16000， 采样位数：16， 声道数： MONO
                     * 100ms必须是音频切片长度的整数倍。切片长度可以是：10ms, 20ms, 50ms, 100ms
                     * dataLength一定要是有效数据长度，否则精确性有影响
                     */
                    RCGameEngine.getInstance().pushAudio(input, input.limit());
                    Log.e("===============", input.limit() + " " + rcRTCAudioFrame.getChannels() + " " + rcRTCAudioFrame.getSampleRate());
                }
            });
            return rcRTCAudioFrame.getBytes();
        }
    };

    @Override
    public void onExpireCode() {
        // 过期后需要调登录重新获取code并更新
        login(new LoginCallback() {
            @Override
            public void onSuccess(String code) {
                // 更新appCode
                RCGameEngine.getInstance().updateAppCode(code);
            }

            @Override
            public void onError() {

            }
        });
    }

    @Override
    public void onGameSettle(RCGameSettle gameSettle) {

    }

    // ==================你画我猜玩家状态监听回调================
    @Override
    public void onSelecting(String userId, boolean isSelecting) {
        // 开发者可显示玩家选词状态
    }

    @Override
    public void onPainting(String userId, boolean isPainting) {
        // 开发者可显示玩家绘画状态
    }

    @Override
    public void onErrorAnswer(String userId, String errorAnswer) {
        // 开发者可显示玩家错误答案状态
    }

    @Override
    public void onTotalScore(String userId, String totalScore) {
        // 开发者可显示玩家总得分
    }

    @Override
    public void onScore(String userId, String score) {
        // 开发者可显示玩家本次得分
    }


    private interface LoginCallback {
        void onSuccess(String code);

        void onError();
    }

    /**
     * 语聊房的监听================================
     */
    private RCVoiceRoomEventListener roomEventListener = new RCVoiceRoomEventListener() {
        @Override
        public void onRoomKVReady() {

        }

        @Override
        public void onRoomDestroy() {

        }

        @Override
        public void onRoomInfoUpdate(RCVoiceRoomInfo rcVoiceRoomInfo) {

        }

        @Override
        public void onSeatInfoUpdate(List<RCVoiceSeatInfo> list) {
            gameSeatAdapter.refreshSeat(list);
        }

        @Override
        public void onUserEnterSeat(int i, String s) {

        }

        @Override
        public void onUserLeaveSeat(int i, String s) {
            gameSeatAdapter.removePlayer(s);
        }

        @Override
        public void onSeatMute(int i, boolean b) {

        }

        @Override
        public void onSeatLock(int i, boolean b) {

        }

        @Override
        public void onAudienceEnter(String s) {

        }

        @Override
        public void onAudienceExit(String s) {

        }

        @Override
        public void onSpeakingStateChanged(int i, int i1) {

        }

        @Override
        public void onMessageReceived(Message message) {
            // 当收到消息时,把消息添加到公屏
            MessageContent content = message.getContent();
            if (content instanceof TextMessage) {
                String msg = ((TextMessage) content).getContent();
                if (msg.contains("gameId:")) {
                    String gameId = msg.replace("gameId:", "");
                    RCGameEngine.getInstance().switchGame(gameId);
                    gameSeatAdapter.deleteCaptain();
                    ivSwitch.setVisibility(View.GONE);
                } else {
                    addMessage(content.getUserInfo().getName() + "：" + ((TextMessage) content).getContent());
                }
            }
        }

        @Override
        public void onRoomNotificationReceived(String s, String s1) {

        }

        @Override
        public void onPickSeatReceivedFrom(String s) {

        }

        @Override
        public void onKickSeatReceived(int i) {
            gameSeatAdapter.removePlayer(i);
        }

        @Override
        public void onRequestSeatAccepted() {

        }

        @Override
        public void onRequestSeatRejected() {

        }

        @Override
        public void onRequestSeatListChanged() {

        }

        @Override
        public void onInvitationReceived(String s, String s1, String s2) {

        }

        @Override
        public void onInvitationAccepted(String s) {

        }

        @Override
        public void onInvitationRejected(String s) {

        }

        @Override
        public void onInvitationCancelled(String s) {

        }

        @Override
        public void onUserReceiveKickOutRoom(String s, String s1) {

        }

        @Override
        public void onNetworkStatus(int i) {

        }

        @Override
        public void onPKGoing(RCPKInfo rcpkInfo) {

        }

        @Override
        public void onPKFinish() {

        }

        @Override
        public void onReceivePKInvitation(String s, String s1) {

        }

        @Override
        public void onPKInvitationCanceled(String s, String s1) {

        }

        @Override
        public void onPKInvitationRejected(String s, String s1) {

        }

        @Override
        public void onPKInvitationIgnored(String s, String s1) {

        }
    };
}
