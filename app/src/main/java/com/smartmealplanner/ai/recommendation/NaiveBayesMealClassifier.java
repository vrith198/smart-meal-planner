package com.smartmealplanner.ai.recommendation;

import android.content.Context;
import com.smartmealplanner.ai.model.MealCategory;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Intelligent Systems (IS2) Module: Embedded Naïve Bayes Classifier in Java.
 * Computes conditional posterior probabilities with Laplace smoothing:
 * ln P(C | X) = ln P(C) + sum_j ln P(x_j | C)
 * Runs 100% locally and offline without external libraries or servers.
 */
public class NaiveBayesMealClassifier {

    private final Map<String, Double> classLogPriors = new HashMap<>();
    private final Map<String, Map<String, Double>> featureLogProbs = new HashMap<>();
    private final List<String> classes = new ArrayList<>();

    public NaiveBayesMealClassifier() {
        // Built-in verified baseline parameters
        initializeDefaultParameters();
    }

    /**
     * Initializes parameters from the exported JSON asset trained on ml/data/meal_preferences.csv.
     */
    public void loadFromAssets(Context context) {
        try (InputStream is = context.getAssets().open("naive_bayes_params.json")) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonStr);

            classes.clear();
            classLogPriors.clear();
            featureLogProbs.clear();

            for (int i = 0; i < root.getJSONArray("classes").length(); i++) {
                classes.add(root.getJSONArray("classes").getString(i));
            }

            JSONObject priorsObj = root.getJSONObject("class_priors");
            for (String cls : classes) {
                double prior = priorsObj.optDouble(cls, 0.25);
                classLogPriors.put(cls, Math.log(Math.max(1e-6, prior)));
            }

            JSONObject featProbObj = root.getJSONObject("feature_prob");
            for (String cls : classes) {
                JSONObject clsFeatures = featProbObj.getJSONObject(cls);
                Map<String, Double> fMap = new HashMap<>();
                Iterator<String> keys = clsFeatures.keys();
                while (keys.hasNext()) {
                    String fName = keys.next();
                    double p = clsFeatures.getDouble(fName);
                    fMap.put(fName, Math.log(Math.max(1e-6, p)));
                }
                featureLogProbs.put(cls, fMap);
            }
        } catch (Exception e) {
            // Keep default parameters on asset load issue
            initializeDefaultParameters();
        }
    }

    private void initializeDefaultParameters() {
        classes.clear();
        classes.addAll(Arrays.asList("breakfast", "lunch", "snack", "dinner"));
        for (String c : classes) {
            classLogPriors.put(c, Math.log(0.25));
            featureLogProbs.put(c, new HashMap<>());
        }
    }

    /**
     * Predicts meal category from user preferences and context features.
     */
    public MealCategory predict(String dietaryPref, String cuisinePref, String budget,
                                String fitnessGoal, String activityLevel, String timeOfDay,
                                String proteinPref) {
        Map<String, Double> posteriors = predictProbabilities(dietaryPref, cuisinePref, budget,
                fitnessGoal, activityLevel, timeOfDay, proteinPref);

        String bestClass = "lunch";
        double maxProb = -Double.MAX_VALUE;
        for (Map.Entry<String, Double> entry : posteriors.entrySet()) {
            if (entry.getValue() > maxProb) {
                maxProb = entry.getValue();
                bestClass = entry.getKey();
            }
        }
        return MealCategory.fromString(bestClass);
    }

    /**
     * Calculates normalized posterior probabilities for each meal category.
     */
    public Map<String, Double> predictProbabilities(String dietaryPref, String cuisinePref, String budget,
                                                   String fitnessGoal, String activityLevel, String timeOfDay,
                                                   String proteinPref) {
        List<String> activeFeatures = new ArrayList<>();
        if (dietaryPref != null) activeFeatures.add("dietary_pref_" + dietaryPref);
        if (cuisinePref != null) activeFeatures.add("cuisine_pref_" + cuisinePref);
        if (budget != null) activeFeatures.add("budget_category_" + budget);
        if (fitnessGoal != null) activeFeatures.add("fitness_goal_" + fitnessGoal);
        if (activityLevel != null) activeFeatures.add("activity_level_" + activityLevel);
        if (timeOfDay != null) activeFeatures.add("time_of_day_" + timeOfDay);
        if (proteinPref != null) activeFeatures.add("protein_pref_" + proteinPref);

        Map<String, Double> logScores = new HashMap<>();
        double maxLogScore = -Double.MAX_VALUE;

        for (String cls : classes) {
            double score = classLogPriors.getOrDefault(cls, Math.log(0.25));
            Map<String, Double> fMap = featureLogProbs.get(cls);
            if (fMap != null) {
                for (String feat : activeFeatures) {
                    score += fMap.getOrDefault(feat, Math.log(0.01)); // Laplace smoothed epsilon
                }
            }
            logScores.put(cls, score);
            if (score > maxLogScore) {
                maxLogScore = score;
            }
        }

        // Softmax normalization for numerical stability
        double sumExp = 0.0;
        Map<String, Double> expScores = new HashMap<>();
        for (String cls : classes) {
            double expVal = Math.exp(logScores.get(cls) - maxLogScore);
            expScores.put(cls, expVal);
            sumExp += expVal;
        }

        Map<String, Double> probabilities = new HashMap<>();
        for (String cls : classes) {
            probabilities.put(cls, expScores.get(cls) / Math.max(1e-9, sumExp));
        }
        return probabilities;
    }
}
