package com.smartmealplanner.ai.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a pending cloud sync operation queued while offline.
 */
@Entity(tableName = "sync_queue")
public class SyncQueueEntity {

    @PrimaryKey(autoGenerate = true)
    public long queueId;

    public String entityType; // MEAL, PROFILE, GROCERY, UCB_STATE, PLAN
    public String entityId;
    public String action;     // CREATE, UPDATE, DELETE
    public String payloadJson;
    public long timestampMs;
    public int retryCount;

    public SyncQueueEntity(String entityType, String entityId, String action,
                           String payloadJson, long timestampMs, int retryCount) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.payloadJson = payloadJson;
        this.timestampMs = timestampMs;
        this.retryCount = retryCount;
    }
}
