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
    // ??????9????????????????????????????????????
    private static final int SEAT_COUNT = 9;
    // ???????????????view
    private FrameLayout flContainer;

    // ????????????
    private ImageView ivBg;
    // ??????????????????
    private ImageView ivSwitch;
    // ????????????
    private ImageView ivClose;
    // ???????????????
    private EditText etInput;
    // ?????????????????????
    private GameSeatAdapter gameSeatAdapter;
    // ????????????
    private RecyclerView rvSeat;
    // ????????????
    private RecyclerView rvMessage;
    // ???????????????
    private MessageAdapter messageAdapter;
    // ???????????????
    private ImageView ivMic;

    private TextView tvTitle;

    // ????????????????????????appcode
    private String mAppCode;
    // ??????id
    private String mUserId = UserManager.getUserId();
    // ????????????
    private RCGameInfo mGameInfo;
    // ???????????????
    private boolean isCreate;
    // ??????id
    private String mRoomId;
    // ??????????????????????????????
    private String mKeyword;
    // ?????????????????????
    private RCGameState mGameState = RCGameState.IDLE;
    // ???????????????
    private String mCaptainUserId = "";
    // ?????????????????????mic
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
        // ????????????????????????->????????????
        createOrJoinRoom();
    }

    // ?????????view
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

        // ??????????????????
        Glide.with(this).load(mGameInfo.getLoadingPic()).into(ivBg);
        // ????????????
        ivClose.setOnClickListener(v -> onBackPressed());
        // ??????????????????
        ivSwitch.setOnClickListener(v -> {
            switchGame();
        });
        // ????????????
        gameSeatAdapter = new GameSeatAdapter(this, SEAT_COUNT, this::onClickSeat);
        rvSeat.setAdapter(gameSeatAdapter);
        // ????????????
        messageAdapter = new MessageAdapter(this);
        rvMessage.setAdapter(messageAdapter);

        tvTitle.setText("??????id???" + mRoomId + "    ????????????" + UserManager.getUser().getUserName());
    }

    // ??????????????????
    private void switchGame() {
        if (mGameState != RCGameState.IDLE) {
            ToastUtil.show("?????????????????????????????????");
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
                                // ???????????????????????????????????????????????????????????????,????????????????????????????????????????????????????????????????????????????????????
                                MessageContent messageContent = new TextMessage("gameId:" + mGameInfo.getGameId());
                                RCVoiceRoomEngine.getInstance().sendRoomMessage(messageContent, null);
                                dialog.dismiss();
                            }
                        })
                        .setTitle("????????????")
                        .show();
            }

            @Override
            public void onError(int code, RCGameError error) {

            }
        });

    }

    // ???????????????????????????
    private void createOrJoinRoom() {
        // ???????????????????????????????????????QuickDemo??????????????????????????????????????????
        RCVoiceRoomEngine.getInstance().setVoiceRoomEventListener(roomEventListener);

        // ??????????????????????????????????????????????????????????????????16K????????????
        // ?????????????????????????????????????????????????????????????????????????????????16K?????????
        // RCGameEngine.getInstance().pushAudio(); ?????????????????????16K????????????PCM??????
        RCRTCConfig rcrtcConfig = RCRTCConfig.Builder.create()
                // ??????16K?????????
                .setAudioSampleRate(16000)
                // ???????????????
                .enableStereo(false)
                .build();

        if (isCreate) {
            // ??????????????????
            RCVoiceRoomInfo roomInfo = new RCVoiceRoomInfo();
            // ???????????????
            roomInfo.setSeatCount(SEAT_COUNT);
            // ???????????????????????????id
            roomInfo.setRoomName(mRoomId);
            // ??????????????????
            roomInfo.setFreeEnterSeat(true);

            RCVoiceRoomEngine.getInstance().createAndJoinRoom(rcrtcConfig, mRoomId, roomInfo, new RCVoiceRoomCallback() {
                @Override
                public void onSuccess() {
                    initAndLoadGame();
                    VMLog.d(TAG, "==============??????????????????");
                }

                @Override
                public void onError(int i, String s) {
                    VMLog.e(TAG, "==============?????????????????? code???" + i + " msg: " + s);
                }
            });
        } else {
            RCVoiceRoomEngine.getInstance().joinRoom(rcrtcConfig, mRoomId, new RCVoiceRoomCallback() {
                @Override
                public void onSuccess() {
                    // ?????????????????????
                    RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
                    initAndLoadGame();
                    VMLog.d(TAG, "==============??????????????????");
                }

                @Override
                public void onError(int i, String s) {
                    VMLog.e(TAG, "==============?????????????????? code???" + i + " msg: " + s);
                }
            });
        }
    }


    // ?????????????????????->????????????
    private void initAndLoadGame() {
        // ?????????????????????????????????????????????appCode
        login(new LoginCallback() {
            @Override
            public void onSuccess(String code) {
                mAppCode = code;
                // ????????????
                loadGame();
            }

            @Override
            public void onError() {

            }
        });
    }


    /**
     * ??????????????????????????????code
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

    // ????????????
    private void loadGame() {
        // ????????????,???init?????????loadGame??????????????????
        RCGameEngine.getInstance().setGameStateListener(this);
        RCGameEngine.getInstance().setGamePlayerStateListener(this);
        RCGameEngine.getInstance().setDrawGuessListener(this);
        // ????????????
        RCGameOption option = RCGameOption.builder()
                // ????????????????????????????????????????????????dp
                .setGameSafeRect(new RCGameSafeRect(10, 100, 10, 200))// ???????????????????????????????????????
                // ????????????????????????????????????
                .setGameSound(new RCGameSound(RCGameSound.SoundControl.OPEN, 100))
                // ???????????????UI???????????????
                .setGameUI(RCGameUI.builder()
                        // ???????????????????????????
                        .setLobbyPlayersHide(false)
                        // ???????????????
                        .setVersionHide(true)
                        .build())
                .build();
        RCGameRoomInfo gameRoomInfo = new RCGameRoomInfo();
        gameRoomInfo.setGameId(mGameInfo.getGameId());
        gameRoomInfo.setRoomId(mRoomId);
        gameRoomInfo.setUserId(mUserId);
        gameRoomInfo.setAppCode(mAppCode);
        // ????????????
        RCGameEngine.getInstance().loadGame(this, flContainer, gameRoomInfo, option);
    }

    /**
     * ??????????????????
     *
     * @param view
     */
    public void clickSend(View view) {
        String text = etInput.getText().toString();
        if (!TextUtils.isEmpty(text)) {
            // ???????????????????????????
            addMessage(UserManager.getUser().getUserName() + "???" + text);
            // ???????????????
            etInput.setText("");
            // ????????????????????????
            MessageContent messageContent = TextMessage.obtain(text);
            messageContent.setUserInfo(new UserInfo(UserManager.getUserId(), UserManager.getUser().getUserName(), Uri.parse(UserManager.getUser().getPortrait())));
            RCVoiceRoomEngine.getInstance().sendRoomMessage(messageContent, null);

            // ??????????????????????????????????????????????????????????????????
            if (TextUtils.equals(mKeyword, text)) {
                RCGameEngine.getInstance().hitKeyword(text, null);
            }
            // ????????????????????????????????????
            if (TextUtils.equals(mGameInfo.getGameId(), "1468091457989509190")) {
                RCGameEngine.getInstance().hitKeyword(text, null);
            }
        }
    }

    // ?????????????????????
    public void clickMic(View view) {
        isOpenMic = !isOpenMic;
        RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
        ivMic.setSelected(isOpenMic);
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param message
     */
    private void addMessage(String message) {
        messageAdapter.addMessage(message);
        rvMessage.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
    }

    // ????????????????????????
    private void handleEnterSeat(int seatIndex) {
        mLoadingView.show("?????????");
        // ?????????????????????
        RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
        // ??????
        RCVoiceRoomEngine.getInstance().enterSeat(seatIndex, new RCVoiceRoomCallback() {
            @Override
            public void onSuccess() {
                ToastUtil.show("????????????");
                // ?????????SDK??????AudioScenario?????????MUSIC_CHATROOM???????????????DEFAULT???????????????????????????????????????????????????
                RCRTCEngine.getInstance().getDefaultAudioStream().setAudioQuality(RCRTCParamsType.AudioQuality.MUSIC_HIGH, RCRTCParamsType.AudioScenario.DEFAULT);
                VMLog.d(TAG, "==============enterSeat success");
                mLoadingView.dismiss();
            }

            @Override
            public void onError(int i, String s) {
                ToastUtil.show("????????????");
                VMLog.e(TAG, "==============enterSeat ?????? code???" + i + " msg: " + s);
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
                VMLog.e(TAG, "==============leaveSeat?????? code???" + i + " msg: " + s);
            }
        });
    }

    /**
     * ????????????
     *
     * @param position
     * @param seatPlayer
     */
    @Override
    public void onClickSeat(int position, SeatPlayer seatPlayer) {
        // ???????????????
        if (seatPlayer != null) {
            // ???????????????
            if (TextUtils.equals(mUserId, mCaptainUserId)) {
                // ??????????????????????????????????????????????????????
                if (TextUtils.equals(mUserId, seatPlayer.userId)) {
                    if (mGameState == RCGameState.PLAYING) {
                        new AlertDialog.Builder(this)
                                .setTitle("????????????????????????????????????")
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ????????????
                                        RCGameEngine.getInstance().endGame(null);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {// ????????????????????????????????????
                        new AlertDialog.Builder(this)
                                .setTitle("????????????????????????")
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ?????????????????????
                                        handleLeaveSeat();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                } else {// ??????????????????????????????
                    new AlertDialog.Builder(this)
                            .setTitle("???????????????????????????????????????")
                            .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // ??????
                                    RCVoiceRoomEngine.getInstance().kickUserFromSeat(seatPlayer.userId, null);
                                    RCGameEngine.getInstance().kickPlayer(seatPlayer.userId, null);
                                    dialog.dismiss();
                                }
                            })

                            .setNegativeButton("????????????", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RCGameEngine.getInstance().setCaptain(seatPlayer.userId, null);
                                    dialog.dismiss();
                                }
                            }).show();
                }
            } else {
                // ?????????????????????????????????,???????????????????????????
                if (TextUtils.equals(seatPlayer.userId, mUserId)) {
                    if (mGameState == RCGameState.PLAYING) {
                        new AlertDialog.Builder(this)
                                .setTitle("??????????????????????????????")
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ????????????
                                        RCGameEngine.getInstance().cancelPlayGame(null);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("????????????????????????")
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // ?????????????????????
                                        handleLeaveSeat();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
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
                ToastUtil.show("????????????????????????");
                // RCVoiceRoomEngine.getInstance().switchSeatTo(position, new RCVoiceRoomCallback() {
                //     @Override
                //     public void onSuccess() {
                //         VMLog.d("??????????????????");
                //     }
                //
                //     @Override
                //     public void onError(int i, String s) {
                //         VMLog.d("??????????????????:code:" + i + " msg" + s);
                //     }
                // });
            } else {
                mLoadingView.show("?????????");
                // ???????????????????????????????????????
                RCVoiceRoomEngine.getInstance().disableAudioRecording(!isOpenMic);
                RCVoiceRoomEngine.getInstance().enterSeat(position, new RCVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        ToastUtil.show("????????????");
                        // ?????????SDK??????AudioScenario?????????MUSIC_CHATROOM???????????????DEFAULT???????????????????????????????????????????????????
                        RCRTCEngine.getInstance().getDefaultAudioStream().setAudioQuality(RCRTCParamsType.AudioQuality.MUSIC_HIGH, RCRTCParamsType.AudioScenario.DEFAULT);

                        RCGameEngine.getInstance().joinGame(null);
                        mLoadingView.dismiss();
                    }

                    @Override
                    public void onError(int i, String s) {
                        ToastUtil.show("????????????:" + s);
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
        // ??????????????????????????????????????????????????????????????????
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

    // ==================????????????????????????================

    @Override
    public void onPlayerIn(String userId, boolean isIn, int teamId) {
        // ???????????????/????????????
        VMLog.d(TAG, "onPlayerIn:" + userId + " " + isIn);
        int seatIndex = -1;
        if (isIn) {
            seatIndex = gameSeatAdapter.getEmptySeat(userId);
            gameSeatAdapter.updateSeat(userId, SeatPlayer.PlayerState.JOIN);
        } else {
            seatIndex = gameSeatAdapter.getSeatIndex(userId);
            gameSeatAdapter.updateSeat(userId, SeatPlayer.PlayerState.IDLE);
        }
        // ?????????????????????????????????????????????
        if (TextUtils.equals(userId, mUserId) && seatIndex != -1) {
            // ???????????????
            if (isIn) {
                handleEnterSeat(seatIndex);
            } else {
                // ??????????????????
            }
        }
        // ????????????
        if (!isIn && TextUtils.equals(mCaptainUserId, userId)) {
            mCaptainUserId = "";
            refreshCaptain();
        }
    }

    @Override
    public void onPlayerCaptain(String userId, boolean isCaptain) {
        // ????????????
        VMLog.d(TAG, "onPlayerCaptain:" + userId + " " + isCaptain);
        if (isCaptain) {
            mCaptainUserId = userId;
            refreshCaptain();
        }
    }

    private void refreshCaptain() {
        gameSeatAdapter.setCaptain(mCaptainUserId);
        // ???????????????
        if (TextUtils.equals(mCaptainUserId, mUserId)) {
            ivSwitch.setVisibility(View.VISIBLE);
        } else {
            ivSwitch.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPlayerReady(String userId, boolean isReady) {
        // ????????????/????????????
        VMLog.d(TAG, "onPlayerReady:" + userId + " " + isReady);
        gameSeatAdapter.updateSeat(userId, isReady ? SeatPlayer.PlayerState.READY : SeatPlayer.PlayerState.JOIN);
    }

    @Override
    public void onPlayerPlaying(String userId, boolean isPlaying) {
        // ????????????????????????
        VMLog.d(TAG, "onPlayerPlaying:" + userId + " " + isPlaying);
        gameSeatAdapter.setPlaying(userId, isPlaying);
    }

    @Override
    public void onPlayerChangeSeat(String userId, int from, int to) {
        // ????????????
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

    // ==================????????????????????????================
    @Override
    public void onGameLoaded() {
        // ??????????????????
        VMLog.d(TAG, "onGameLoaded");
    }

    @Override
    public void onGameDestroyed() {
        // ????????????
        VMLog.d(TAG, "onGameDestroyed");
        stopVoiceRecorder();
    }

    @Override
    public void onReceivePublicMessage(String message) {
        // ?????????????????????
        VMLog.d(TAG, "onReceivePublicMessage:" + message);
        addMessage(message);
    }

    @Override
    public void onKeywordToHit(String keyword) {
        // ????????????????????????????????????
        // ???????????????????????????????????????????????????????????????
        // ????????????????????????????????????????????????????????????
        mKeyword = keyword;
    }

    @Override
    public void onGameStateChanged(RCGameState gameState) {
        // ??????????????????
        // gameState=0 (idle ?????????????????????????????????????????????
        // gameState=1 ???loading ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        // gameState=2???playing?????????????????????????????????
        VMLog.d(TAG, "onGameStateChanged:" + gameState);
        mGameState = gameState;
        // TODO ???????????????????????????/???????????????????????????????????????????????????????????????????????????
        // ??????????????????????????????
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
     * ???????????????????????????
     *
     * @param isOpen ????????????
     */
    @Override
    public void onGameASRChanged(boolean isOpen) {
        VMLog.d("???????????????????????????????????????????????? " + isOpen);
        // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

        if (isOpen) {
            startVoiceRecorder();
        } else {
            stopVoiceRecorder();
        }
    }


    private void startVoiceRecorder() {
        // ?????????????????????
        RCRTCEngine.getInstance().getDefaultAudioStream().setRecordAudioDataListener(dataListener);
    }

    private void stopVoiceRecorder() {
        // ?????????????????????
        if (RCRTCEngine.getInstance().getDefaultAudioStream() != null) {
            RCRTCEngine.getInstance().getDefaultAudioStream().setRecordAudioDataListener(null);
        }
    }

    private IRCRTCAudioDataListener dataListener = new IRCRTCAudioDataListener() {
        // ?????????10ms????????????????????????????????????????????????????????????????????????????????????pushAudio
        @Override
        public byte[] onAudioFrame(RCRTCAudioFrame rcRTCAudioFrame) {
            // ???????????????16K????????????????????????????????????????????????????????????
            ByteBuffer input = ByteBuffer.wrap(rcRTCAudioFrame.getBytes());
            UIKit.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /**
                     * ???????????????????????????????????????PCM?????????????????????16000??? ???????????????16??? ???????????? MONO
                     * 100ms??????????????????????????????????????????????????????????????????10ms, 20ms, 50ms, 100ms
                     * dataLength?????????????????????????????????????????????????????????
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
        // ????????????????????????????????????code?????????
        login(new LoginCallback() {
            @Override
            public void onSuccess(String code) {
                // ??????appCode
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

    // ==================????????????????????????????????????================
    @Override
    public void onSelecting(String userId, boolean isSelecting) {
        // ????????????????????????????????????
    }

    @Override
    public void onPainting(String userId, boolean isPainting) {
        // ????????????????????????????????????
    }

    @Override
    public void onErrorAnswer(String userId, String errorAnswer) {
        // ??????????????????????????????????????????
    }

    @Override
    public void onTotalScore(String userId, String totalScore) {
        // ?????????????????????????????????
    }

    @Override
    public void onScore(String userId, String score) {
        // ????????????????????????????????????
    }


    private interface LoginCallback {
        void onSuccess(String code);

        void onError();
    }

    /**
     * ??????????????????================================
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
            // ??????????????????,????????????????????????
            MessageContent content = message.getContent();
            if (content instanceof TextMessage) {
                String msg = ((TextMessage) content).getContent();
                if (msg.contains("gameId:")) {
                    String gameId = msg.replace("gameId:", "");
                    RCGameEngine.getInstance().switchGame(gameId);
                    gameSeatAdapter.deleteCaptain();
                    ivSwitch.setVisibility(View.GONE);
                } else {
                    addMessage(content.getUserInfo().getName() + "???" + ((TextMessage) content).getContent());
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
