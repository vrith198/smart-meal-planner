package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.smartmealplanner.ai.database.entity.LoggedMealEntity;

import java.util.List;

@Dao
public interface LoggedMealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LoggedMealEntity meal);

    @Update
    void update(LoggedMealEntity meal);

    @Delete
    void delete(LoggedMealEntity meal);

    @Query("DELETE FROM logged_meals WHERE mealId = :mealId")
    void deleteById(String mealId);

    @Query("SELECT * FROM logged_meals WHERE mealId = :mealId LIMIT 1")
    LoggedMealEntity getMealById(String mealId);

    @Query("SELECT * FROM logged_meals WHERE userId = :userId AND dateString = :dateString ORDER BY timestampMs DESC")
    LiveData<List<LoggedMealEntity>> getMealsForDateLive(String userId, String dateString);

    @Query("SELECT * FROM logged_meals WHERE userId = :userId AND dateString = :dateString ORDER BY timestampMs DESC")
    List<LoggedMealEntity> getMealsForDateSync(String userId, String dateString);

    @Query("SELECT * FROM logged_meals WHERE userId = :userId ORDER BY timestampMs DESC")
    LiveData<List<LoggedMealEntity>> getAllMealsLive(String userId);

    @Query("SELECT * FROM logged_meals WHERE userId = :userId ORDER BY timestampMs DESC")
    List<LoggedMealEntity> getAllMealsSync(String userId);

    @Query("SELECT * FROM logged_meals WHERE syncState != 0")
    List<LoggedMealEntity> getPendingSyncMeals();

    @Query("UPDATE logged_meals SET syncState = 0 WHERE mealId = :mealId")
    void markAsSynced(String mealId);

    @Query("SELECT SUM(calories) FROM logged_meals WHERE userId = :userId AND dateString = :dateString")
    LiveData<Double> getTotalCaloriesForDateLive(String userId, String dateString);
}
