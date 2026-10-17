package com.smartmealplanner.ai.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.smartmealplanner.ai.database.dao.*;
import com.smartmealplanner.ai.database.entity.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        FoodItemEntity.class,
        LoggedMealEntity.class,
        MealPlanEntity.class,
        GroceryItemEntity.class,
        UCB1StateEntity.class,
        UserProfileEntity.class,
        SyncQueueEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DB_NAME = "smart_meal_planner.db";
    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public abstract FoodItemDao foodItemDao();
    public abstract LoggedMealDao loggedMealDao();
    public abstract MealPlanDao mealPlanDao();
    public abstract GroceryDao groceryDao();
    public abstract UCB1Dao ucb1Dao();
    public abstract UserDao userDao();
    public abstract SyncQueueDao syncQueueDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DB_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            databaseWriteExecutor.execute(() -> {
                                prePopulateDatabase(context.getApplicationContext());
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Reads verified food nutrition records from assets/food_nutrition_db.json
     * and seeds the local Room database for 100% offline functionality.
     */
    public static void prePopulateDatabase(Context context) {
        try {
            AppDatabase database = getInstance(context);
            if (database.foodItemDao().getFoodCount() > 0) {
                return; // Already populated
            }

            Log.d(TAG, "Pre-populating database with verified food nutrition catalog...");
            InputStream is = context.getAssets().open("food_nutrition_db.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);

            JSONArray array = new JSONArray(jsonStr);
            List<FoodItemEntity> foods = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.getString("name");
                String label = obj.getString("classifierLabel");
                String diet = obj.getString("dietaryType");
                String category = obj.getString("mealCategory");
                double cal100 = obj.getDouble("caloriesPer100g");
                double prot100 = obj.getDouble("proteinPer100g");
                double carb100 = obj.getDouble("carbsPer100g");
                double fat100 = obj.getDouble("fatPer100g");
                int smallG = obj.optInt("smallServingGrams", 60);
                int medG = obj.optInt("mediumServingGrams", 120);
                int largeG = obj.optInt("largeServingGrams", 200);
                String unit = obj.optString("servingUnitName", "Serving");
                double cost = obj.optDouble("estimatedCostInr", 50.0);
                String allergens = obj.optJSONArray("allergens") != null ? obj.optJSONArray("allergens").toString() : "[]";
                String ingredients = obj.optJSONArray("ingredients") != null ? obj.optJSONArray("ingredients").toString() : "[]";
                String source = obj.optString("source", "USDA / IFCT");

                foods.add(new FoodItemEntity(
                    id, name, label, diet, category, cal100, prot100, carb100, fat100,
                    smallG, medG, largeG, unit, cost, allergens, ingredients, source
                ));
            }

            database.foodItemDao().insertAll(foods);
            Log.d(TAG, "Successfully seeded " + foods.size() + " foods into Room database.");

            // Also seed default offline guest profile if absent
            String guestUserId = "guest_user";
            UserProfileEntity existing = database.userDao().getUserProfileSync(guestUserId);
            if (existing == null) {
                UserProfileEntity defaultProfile = new UserProfileEntity(
                    guestUserId, "Guest User", "guest@smartmealplanner.local",
                    25, 172.0, 68.0, true, "Moderate",
                    "VEGETARIAN", "MAINTENANCE", "MEDIUM", "North Indian",
                    2100.0, 105.0, 260.0, 70.0, "[]",
                    0, System.currentTimeMillis()
                );
                database.userDao().insertOrUpdate(defaultProfile);
                Log.d(TAG, "Default guest profile initialized.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error pre-populating Room database: " + e.getMessage(), e);
        }
    }
}
