package com.smartmealplanner.ai.data.repository;

import android.content.Context;
import android.util.Log;

import com.smartmealplanner.ai.model.IngredientSubstitute;
import com.smartmealplanner.ai.model.SubstituteOption;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IngredientSubstituteRepository {
    private static final String TAG = "SubstituteRepo";
    private static final String ASSET_NAME = "ingredient_substitutes.json";

    private static volatile IngredientSubstituteRepository instance;
    private final Context appContext;
    private List<IngredientSubstitute> substituteList;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "fresh", "thick", "organic", "chopped", "crushed", "roasted", "steamed",
            "finely", "sliced", "diced", "powder", "paste", "puree", "and", "with",
            "raw", "cooked", "dry", "hot", "warm", "cold", "pieces", "tablespoon", "teaspoon", "cup"
    ));

    private IngredientSubstituteRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static IngredientSubstituteRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (IngredientSubstituteRepository.class) {
                if (instance == null) {
                    instance = new IngredientSubstituteRepository(context);
                }
            }
        }
        return instance;
    }

    public synchronized List<IngredientSubstitute> getAllSubstitutes() {
        if (substituteList == null) {
            substituteList = loadFromAssets();
        }
        return substituteList;
    }

    /**
     * Finds the best matching substitute entry for the given ingredient name.
     */
    public IngredientSubstitute findSubstituteFor(String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            return null;
        }

        List<IngredientSubstitute> list = getAllSubstitutes();

        // 1. Exact match against canonical name or aliases
        for (IngredientSubstitute item : list) {
            if (item.matches(ingredientName)) {
                return item;
            }
        }

        // 2. Token / Keyword fallback search (e.g. "Potatoes & Onions", "Toor Dal (Lentils)")
        String cleanQuery = ingredientName.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] tokens = cleanQuery.split(" ");
        for (String token : tokens) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                for (IngredientSubstitute item : list) {
                    if (item.matches(token)) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns list of substitute options, or empty list if none matched.
     */
    public List<SubstituteOption> getOptionsFor(String ingredientName) {
        IngredientSubstitute sub = findSubstituteFor(ingredientName);
        if (sub != null && sub.getSubstitutes() != null) {
            return sub.getSubstitutes();
        }
        return Collections.emptyList();
    }

    private List<IngredientSubstitute> loadFromAssets() {
        List<IngredientSubstitute> results = new ArrayList<>();
        try {
            InputStream is = appContext.getAssets().open(ASSET_NAME);
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);
            is.close();

            if (read > 0) {
                String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                JSONArray array = new JSONArray(jsonStr);

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String canonicalName = obj.optString("canonicalName");
                    String category = obj.optString("category", "General");

                    List<String> aliases = new ArrayList<>();
                    JSONArray aliasArr = obj.optJSONArray("aliases");
                    if (aliasArr != null) {
                        for (int a = 0; a < aliasArr.length(); a++) {
                            aliases.add(aliasArr.getString(a));
                        }
                    }

                    List<SubstituteOption> options = new ArrayList<>();
                    JSONArray subArr = obj.optJSONArray("substitutes");
                    if (subArr != null) {
                        for (int s = 0; s < subArr.length(); s++) {
                            JSONObject subObj = subArr.getJSONObject(s);
                            SubstituteOption opt = new SubstituteOption(
                                    subObj.optString("name"),
                                    subObj.optString("ratio"),
                                    subObj.optString("dietaryTag"),
                                    subObj.optString("culinaryRole"),
                                    subObj.optString("reason"),
                                    subObj.optString("sourceName"),
                                    subObj.optString("sourceUrl")
                            );
                            options.add(opt);
                        }
                    }

                    results.add(new IngredientSubstitute(canonicalName, category, aliases, options));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading ingredient substitutes from assets", e);
        }
        return results;
    }
}
