package cn.rongcloud.tinygame.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.rongcloud.tinygame.R;
import cn.rongcloud.tinygame.model.SeatPlayer;
import cn.rongcloud.voiceroom.model.RCVoiceSeatInfo;

/**
 * @author gyn
 * @date 2022/3/4
 */
public class GameSeatAdapter extends RecyclerView.Adapter<GameSeatAdapter.SeatViewHolder> {
    private Map<Integer, SeatPlayer> seatPlayerMap;
    // 存放userId->未准备/已准备
    private Map<String, SeatPlayer.PlayerState> stateMap;
    private int maxSeat = 9;
    private Context context;
    private OnSeatClickListener onSeatClickListener;
    private String mCaptainUserId;

    public GameSeatAdapter(Context context, int maxSeat, OnSeatClickListener onSeatClickListener) {
        this.context = context;
        this.maxSeat = maxSeat;
        this.onSeatClickListener = onSeatClickListener;
        seatPlayerMap = new LinkedHashMap<>();
        stateMap = new HashMap<>();
    }

    public int getEmptySeat(String userId) {
        if (existSeat(userId)) {
            return -1;
        }
        int position = 0;
        while (position < maxSeat) {
            if (!seatPlayerMap.containsKey(position)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public int getSeatIndex(String userId) {
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            SeatPlayer player = entry.getValue();
            if (TextUtils.equals(userId, player.userId)) {
                int position = entry.getKey();
                return position;
            }
        }
        return -1;
    }

    public int addPlayer(String userId, int position) {
        if (seatPlayerMap.size() >= maxSeat) {
            return -1;
        }

        if (existSeat(userId)) {
            return -1;
        }
        SeatPlayer player = new SeatPlayer();
        player.userId = userId;
        player.isCaptain = TextUtils.equals(userId, mCaptainUserId);
        player.isShowTotalScore = false;
        player.totalScore = "";
        SeatPlayer.PlayerState state = stateMap.get(userId);
        player.playerState = state != null ? state : SeatPlayer.PlayerState.IDLE;
        seatPlayerMap.put(position, player);
        notifyDataSetChanged();
        return position;
    }

    public int removePlayer(String userId) {
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            SeatPlayer player = entry.getValue();
            if (TextUtils.equals(userId, player.userId)) {
                int position = entry.getKey();
                seatPlayerMap.remove(position);
                notifyDataSetChanged();
                return position;
            }
        }
        return -1;
    }

    public void removePlayer(int index) {
        seatPlayerMap.remove(index);
        notifyDataSetChanged();
    }


    public boolean existSeat(String userId) {
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            SeatPlayer player = entry.getValue();
            if (TextUtils.equals(userId, player.userId)) {
                return true;
            }
        }

        return false;
    }

    public void updateSeat(String userId, SeatPlayer.PlayerState playerState) {
        stateMap.put(userId, playerState);
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            SeatPlayer player = entry.getValue();
            if (TextUtils.equals(userId, player.userId)) {
                player.playerState = playerState;
                int position = entry.getKey();
                notifyItemChanged(position);
                break;
            }
        }
    }

    public void deleteCaptain() {
        SeatPlayer player;
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            player = entry.getValue();
            player.isCaptain = false;
            player.playerState = SeatPlayer.PlayerState.IDLE;
        }
        notifyDataSetChanged();
    }

    public void setCaptain(String userId) {
        mCaptainUserId = userId;
        SeatPlayer player;
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            player = entry.getValue();
            player.isCaptain = TextUtils.equals(player.userId, mCaptainUserId);
        }
        notifyDataSetChanged();
    }

    public void setPlaying(String userId, boolean isPlaying) {
        for (Map.Entry<Integer, SeatPlayer> entry : seatPlayerMap.entrySet()) {
            SeatPlayer player = entry.getValue();
            if (TextUtils.equals(userId, player.userId)) {
                player.playerState = isPlaying ? SeatPlayer.PlayerState.PLAY : (stateMap.get(userId) == null ? SeatPlayer.PlayerState.IDLE : stateMap.get(userId));
                int position = entry.getKey();
                notifyItemChanged(position);
                break;
            }
        }
    }

    public void refreshSeat(List<RCVoiceSeatInfo> seatInfoList) {
        if (seatInfoList != null && seatInfoList.size() > 0) {
            RCVoiceSeatInfo seatInfo;
            for (int i = 0; i < seatInfoList.size(); i++) {
                seatInfo = seatInfoList.get(i);
                if (seatInfo.getStatus() == RCVoiceSeatInfo.RCSeatStatus.RCSeatStatusUsing) {
                    if (!existSeat(seatInfo.getUserId())) {
                        addPlayer(seatInfo.getUserId(), i);
                    }
                }
            }
        } else {
            seatPlayerMap.clear();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SeatViewHolder(LayoutInflater.from(context).inflate(R.layout.item_seat, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        SeatPlayer seatPlayer = seatPlayerMap.get(position);
        if (seatPlayer != null) {
            holder.setPlayer(seatPlayer);
        } else {
            holder.setEmpty();
        }
        holder.itemView.setOnClickListener(v -> {
            if (onSeatClickListener != null)
                onSeatClickListener.onClickSeat(position, seatPlayer);
        });
    }

    @Override
    public int getItemCount() {
        return maxSeat;
    }

    public class SeatViewHolder extends RecyclerView.ViewHolder {
        private RoundedImageView ivAvatar;
        private ImageView seatUsrCaptain;
        private TextView tvState;
        private TextView tvName;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = (RoundedImageView) itemView.findViewById(R.id.iv_avatar);
            seatUsrCaptain = (ImageView) itemView.findViewById(R.id.seat_usr_captain);
            tvState = (TextView) itemView.findViewById(R.id.tv_state);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
        }

        public void setPlayer(SeatPlayer seatPlayer) {
            Glide.with(context).load(seatPlayer.avatarUrl).error(R.drawable.img_avatar).into(ivAvatar);
            seatUsrCaptain.setVisibility(View.VISIBLE);
            tvState.setVisibility(View.VISIBLE);
            tvName.setVisibility(View.VISIBLE);

            tvState.setText(seatPlayer.playerState.desc);
            if (seatPlayer.playerState == SeatPlayer.PlayerState.IDLE) {
                tvState.setVisibility(View.INVISIBLE);
            } else if (seatPlayer.playerState == SeatPlayer.PlayerState.JOIN) {
                tvState.setVisibility(View.VISIBLE);
                tvState.setBackgroundResource(R.drawable.bg_seat_unready);
            } else {
                tvState.setVisibility(View.VISIBLE);
                tvState.setBackgroundResource(R.drawable.bg_seat_ready);
            }
            seatUsrCaptain.setVisibility(seatPlayer.isCaptain ? View.VISIBLE : View.GONE);
            tvName.setText(seatPlayer.userId);
        }

        public void setEmpty() {
            Glide.with(context).load(R.drawable.ic_enter_seat).into(ivAvatar);
            seatUsrCaptain.setVisibility(View.INVISIBLE);
            tvState.setVisibility(View.INVISIBLE);
            tvName.setVisibility(View.INVISIBLE);
        }
    }

    public interface OnSeatClickListener {
        void onClickSeat(int position, SeatPlayer seatPlayer);
    }
}
