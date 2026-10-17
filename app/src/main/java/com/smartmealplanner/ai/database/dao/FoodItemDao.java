package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartmealplanner.ai.database.entity.FoodItemEntity;

import java.util.List;

@Dao
public interface FoodItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FoodItemEntity> foods);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodItemEntity food);

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    LiveData<List<FoodItemEntity>> getAllFoodsLive();

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    List<FoodItemEntity> getAllFoodsSync();

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    FoodItemEntity getFoodById(String id);

    @Query("SELECT * FROM food_items WHERE classifierLabel = :label LIMIT 1")
    FoodItemEntity getFoodByClassifierLabel(String label);

    @Query("SELECT * FROM food_items WHERE mealCategory = :category")
    List<FoodItemEntity> getFoodsByCategory(String category);

    @Query("SELECT COUNT(*) FROM food_items")
    int getFoodCount();
}
