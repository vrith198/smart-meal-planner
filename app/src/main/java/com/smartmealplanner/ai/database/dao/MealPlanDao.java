package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartmealplanner.ai.database.entity.MealPlanEntity;

import java.util.List;

@Dao
public interface MealPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(MealPlanEntity plan);

    @Query("SELECT * FROM meal_plans WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    LiveData<MealPlanEntity> getPlanForDateLive(String userId, String dateString);

    @Query("SELECT * FROM meal_plans WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    MealPlanEntity getPlanForDateSync(String userId, String dateString);

    @Query("SELECT * FROM meal_plans WHERE syncState != 0")
    List<MealPlanEntity> getPendingSyncPlans();

    @Query("UPDATE meal_plans SET syncState = 0 WHERE planId = :planId")
    void markAsSynced(String planId);
}
