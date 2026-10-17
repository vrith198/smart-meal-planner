package com.smartmealplanner.ai.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.smartmealplanner.ai.database.entity.SyncQueueEntity;

import java.util.List;

@Dao
public interface SyncQueueDao {

    @Insert
    long enqueue(SyncQueueEntity item);

    @Query("SELECT * FROM sync_queue ORDER BY timestampMs ASC LIMIT :limit")
    List<SyncQueueEntity> getNextBatch(int limit);

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    void deleteById(long queueId);

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE queueId = :queueId")
    void incrementRetry(long queueId);

    @Query("SELECT COUNT(*) FROM sync_queue")
    int getPendingCount();
}
