package com.smartmealplanner.ai.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a grocery list item.
 */
@Entity(
    tableName = "grocery_items",
    indices = {
        @Index(value = {"userId"}),
        @Index(value = {"syncState"})
    }
)
public class GroceryItemEntity {

    public static final int SYNCED = 0;
    public static final int PENDING_CREATE = 1;
    public static final int PENDING_UPDATE = 2;
    public static final int PENDING_DELETE = 3;

    @PrimaryKey
    @NonNull
    public String itemId; // UUID

    @NonNull
    public String userId;

    @NonNull
    public String ingredientName;

    public double quantity;
    public String unit;
    public String category; // Produce, Grains, Dairy, Spices, Pantry
    public boolean isChecked;

    public int syncState;
    public long updatedAtMs;

    public GroceryItemEntity(@NonNull String itemId, @NonNull String userId, @NonNull String ingredientName,
                             double quantity, String unit, String category, boolean isChecked,
                             int syncState, long updatedAtMs) {
        this.itemId = itemId;
        this.userId = userId;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.isChecked = isChecked;
        this.syncState = syncState;
        this.updatedAtMs = updatedAtMs;
    }
}
