package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.smartmealplanner.ai.database.entity.GroceryItemEntity;

import java.util.List;

@Dao
public interface GroceryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GroceryItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GroceryItemEntity item);

    @Update
    void update(GroceryItemEntity item);

    @Delete
    void delete(GroceryItemEntity item);

    @Query("DELETE FROM grocery_items WHERE itemId = :itemId")
    void deleteById(String itemId);

    @Query("SELECT * FROM grocery_items WHERE userId = :userId ORDER BY isChecked ASC, category ASC, ingredientName ASC")
    LiveData<List<GroceryItemEntity>> getAllItemsLive(String userId);

    @Query("SELECT * FROM grocery_items WHERE userId = :userId ORDER BY isChecked ASC, category ASC, ingredientName ASC")
    List<GroceryItemEntity> getAllItemsSync(String userId);

    @Query("SELECT * FROM grocery_items WHERE syncState != 0")
    List<GroceryItemEntity> getPendingSyncItems();

    @Query("UPDATE grocery_items SET isChecked = :isChecked, syncState = 2, updatedAtMs = :timestamp WHERE itemId = :itemId")
    void toggleChecked(String itemId, boolean isChecked, long timestamp);

    @Query("DELETE FROM grocery_items WHERE userId = :userId AND isChecked = 1")
    void clearCompleted(String userId);

    @Query("UPDATE grocery_items SET syncState = 0 WHERE itemId = :itemId")
    void markAsSynced(String itemId);
}
