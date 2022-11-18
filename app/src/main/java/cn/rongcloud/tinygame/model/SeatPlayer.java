/*
 * Copyright © Sud.Tech
 * https://sud.tech
 */

package cn.rongcloud.tinygame.model;

public class SeatPlayer {
    public String userId;

    public String avatarUrl;

    public boolean isCaptain;

    public boolean isShowTotalScore;

    public String totalScore;

    public PlayerState playerState = PlayerState.IDLE;

    public enum PlayerState {
        IDLE(""), JOIN("未准备"), READY("已准备"), PLAY("游戏中");
        public String desc;

        PlayerState(String desc) {
            this.desc = desc;
        }
    }
}
