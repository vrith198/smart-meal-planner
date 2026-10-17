package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity tracking multi-armed bandit state per food arm for the user.
 */
@Entity(
    tableName = "ucb1_states",
    primaryKeys = {"armId", "userId"},
    indices = {
        @Index(value = {"userId"}),
        @Index(value = {"syncState"})
    }
)
public class UCB1StateEntity {

    @NonNull
    public String armId; // Food item id

    @NonNull
    public String userId;

    public int selections;     // n_i
    public double totalReward; // W_i
    public double meanReward;  // r_bar_i

    public long lastUpdatedMs;
    public int syncState;

    public UCB1StateEntity(@NonNull String armId, @NonNull String userId, int selections,
                           double totalReward, double meanReward, long lastUpdatedMs, int syncState) {
        this.armId = armId;
        this.userId = userId;
        this.selections = selections;
        this.totalReward = totalReward;
        this.meanReward = meanReward;
        this.lastUpdatedMs = lastUpdatedMs;
        this.syncState = syncState;
    }
}
