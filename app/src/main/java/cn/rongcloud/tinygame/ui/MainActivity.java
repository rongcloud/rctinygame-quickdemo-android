package cn.rongcloud.tinygame.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.rongcloud.gamelib.api.RCGameEngine;
import cn.rongcloud.gamelib.callback.RCGameListCallback;
import cn.rongcloud.gamelib.error.RCGameError;
import cn.rongcloud.gamelib.model.RCGameConfig;
import cn.rongcloud.gamelib.model.RCGameInfo;
import cn.rongcloud.tinygame.BuildConfig;
import cn.rongcloud.tinygame.R;
import cn.rongcloud.tinygame.utils.PermissionUtil;
import cn.rongcloud.tinygame.utils.ToastUtil;

public class MainActivity extends AppCompatActivity {
    protected final static String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private RecyclerView rvGameList;
    private InputDialogFragment inputDialogFragment;
    private RadioGroup rgLanguage;
    GameAdapter adapter;
    private RadioButton rbEn;
    private RadioButton rbZh;

    public static void show(Context context) {
        context.startActivity(new Intent(context, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }


    private void initView() {
        rvGameList = (RecyclerView) findViewById(R.id.rv_game_list);
        adapter = new GameAdapter(this);
        rvGameList.setAdapter(adapter);

        rgLanguage = (RadioGroup) findViewById(R.id.rg_language);
        rbEn = (RadioButton) findViewById(R.id.rb_en);
        rbZh = (RadioButton) findViewById(R.id.rb_zh);

        rgLanguage.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Locale locale;
                if (checkedId == R.id.rb_en) {
                    locale = Locale.US;
                } else {
                    locale = Locale.CHINA;
                }
                RCGameEngine.getInstance().setGameConfig(RCGameConfig.builder().setGameLanguage(locale).setDebug(BuildConfig.DEBUG).build());
                getGameList();
            }
        });
        rbEn.setChecked(true);
    }

    private void getGameList() {
        RCGameEngine.getInstance().getGameList(new RCGameListCallback() {
            @Override
            public void onSuccess(List<RCGameInfo> gameInfoList) {
                adapter.setGameInfoList(gameInfoList);
            }

            @Override
            public void onError(int code, RCGameError error) {

            }
        });
    }

    private class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {
        List<RCGameInfo> gameInfoList = new ArrayList<>();
        private Context context;

        public GameAdapter(Context context) {
            this.context = context;
        }

        public void setGameInfoList(List<RCGameInfo> gameInfoList) {
            if (gameInfoList != null) {
                this.gameInfoList = gameInfoList;
                notifyDataSetChanged();
            }
        }

        @NonNull
        @Override
        public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GameViewHolder(LayoutInflater.from(context).inflate(R.layout.item_game, parent, false));
        }

        @Override
        public int getItemCount() {
            return gameInfoList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            RCGameInfo gameInfo = gameInfoList.get(position);
            holder.setGameInfo(position, gameInfo);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        private class GameViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivImage;
            private TextView tvIndex;
            private TextView tvName;
            private TextView tvId;
            private TextView tvPlayerSize;
            private TextView tvCreate;
            private TextView tvJoin;

            public GameViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = (ImageView) itemView.findViewById(R.id.iv_image);
                tvIndex = (TextView) itemView.findViewById(R.id.tv_index);
                tvName = (TextView) itemView.findViewById(R.id.tv_name);
                tvId = (TextView) itemView.findViewById(R.id.tv_id);
                tvPlayerSize = (TextView) itemView.findViewById(R.id.tv_player_size);
                tvCreate = itemView.findViewById(R.id.tv_create);
                tvJoin = itemView.findViewById(R.id.tv_join);
            }

            public void setGameInfo(int position, RCGameInfo gameInfo) {
                tvIndex.setText(position + "");
                Glide.with(context).load(gameInfo.getThumbnail()).into(ivImage);
                tvName.setText(gameInfo.getGameName());
                tvId.setText("游戏id：" + gameInfo.getGameId());
                tvPlayerSize.setText("游戏人数：" + gameInfo.getMinSeat() + " ~ " + gameInfo.getMaxSeat());
                tvCreate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!PermissionUtil.checkPermissions(MainActivity.this, PERMISSIONS)) {
                            return;
                        }

                        inputDialogFragment = new InputDialogFragment(new InputDialogFragment.OnButtonClickListener() {
                            @Override
                            public void clickConfirm(String text) {
                                if (TextUtils.isEmpty(text)) {
                                    ToastUtil.show("请输入房间id");
                                    return;
                                }
                                GameActivity.launch(context, gameInfo, text, true);
                            }
                        });
                        inputDialogFragment.show(getSupportFragmentManager());
                    }
                });
                tvJoin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!PermissionUtil.checkPermissions(MainActivity.this, PERMISSIONS)) {
                            return;
                        }
                        inputDialogFragment = new InputDialogFragment(new InputDialogFragment.OnButtonClickListener() {
                            @Override
                            public void clickConfirm(String text) {
                                if (TextUtils.isEmpty(text)) {
                                    ToastUtil.show("请输入房间id");
                                    return;
                                }
                                GameActivity.launch(context, gameInfo, text, false);
                            }
                        });
                        inputDialogFragment.show(getSupportFragmentManager());
                    }
                });
            }
        }
    }
}