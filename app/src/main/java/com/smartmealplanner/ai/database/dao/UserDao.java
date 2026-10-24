package com.smartmealplanner.ai.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.smartmealplanner.ai.database.entity.UserProfileEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserProfileEntity profile);

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    LiveData<UserProfileEntity> getUserProfileLive(String userId);

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    UserProfileEntity getUserProfileSync(String userId);

    @Query("SELECT * FROM user_profiles LIMIT 1")
    UserProfileEntity getUserProfileSync();

    @Query("SELECT * FROM user_profiles LIMIT 1")
    UserProfileEntity getUserProfileSync();

    @Query("SELECT * FROM user_profiles WHERE syncState != 0")
    List<UserProfileEntity> getPendingSyncProfiles();

    @Query("UPDATE user_profiles SET syncState = 0 WHERE userId = :userId")
    void markAsSynced(String userId);
}
