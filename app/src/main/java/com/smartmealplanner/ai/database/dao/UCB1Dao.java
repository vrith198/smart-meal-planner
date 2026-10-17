package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartmealplanner.ai.database.entity.UCB1StateEntity;

import java.util.List;

@Dao
public interface UCB1Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UCB1StateEntity state);

    @Query("SELECT * FROM ucb1_states WHERE armId = :armId AND userId = :userId LIMIT 1")
    UCB1StateEntity getArmState(String armId, String userId);

    @Query("SELECT * FROM ucb1_states WHERE userId = :userId")
    List<UCB1StateEntity> getAllArmsSync(String userId);

    @Query("SELECT * FROM ucb1_states WHERE userId = :userId")
    LiveData<List<UCB1StateEntity>> getAllArmsLive(String userId);

    @Query("SELECT COALESCE(SUM(selections), 0) FROM ucb1_states WHERE userId = :userId")
    int getTotalSelections(String userId);

    @Query("SELECT * FROM ucb1_states WHERE syncState != 0")
    List<UCB1StateEntity> getPendingSyncStates();

    @Query("UPDATE ucb1_states SET syncState = 0 WHERE armId = :armId AND userId = :userId")
    void markAsSynced(String armId, String userId);
}
