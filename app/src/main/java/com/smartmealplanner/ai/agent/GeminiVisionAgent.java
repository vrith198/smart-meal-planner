package com.smartmealplanner.ai.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.smartmealplanner.ai.model.PredictionResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemini Multimodal Vision Agent.
 * Sends meal photos to Google Gemini 1.5 Flash multimodal endpoint to analyze:
 * - Complex or uncatalogued dishes (restaurant foods, home cooking)
 * - Calorie estimation (kcal)
 * - Macronutrient breakdown (Protein, Carbs, Fat)
 * - Portion size assessment
 * - Ingredient breakdown and dietary health insights.
 */
public class GeminiVisionAgent {

    private static final String TAG = "GeminiVisionAgent";
    private static final String PREFS_NAME = "gemini_agent_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";

    // Recommended stable multimodal model
    private static final String MODEL_NAME = "gemini-1.5-flash";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/" 
            + MODEL_NAME + ":generateContent?key=";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AgentCallback {
        void onSuccess(PredictionResult result);
        void onError(String errorMessage);
    }

    public static boolean hasApiKey(Context context) {
        String key = getApiKey(context);
        return key != null && !key.trim().isEmpty();
    }

    public static String getApiKey(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_GEMINI_API_KEY, "");
    }

    public static void setApiKey(Context context, String apiKey) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey != null ? apiKey.trim() : "").apply();
    }

    public static void clearApiKey(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply();
    }

    /**
     * Analyzes a meal photo asynchronously via the Gemini 1.5 Flash Vision Agent.
     */
    public void analyzeMealPhoto(Context context, Bitmap originalBitmap, AgentCallback callback) {
        if (originalBitmap == null) {
            callback.onError("No image provided for Gemini Vision analysis.");
            return;
        }

        final String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("API_KEY_REQUIRED");
            return;
        }

        executor.execute(() -> {
            try {
                // 1. Prepare and optimize image bitmap
                Bitmap scaledBitmap = scaleBitmapDown(originalBitmap, 768);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                // 2. Build JSON Request payload
                JSONObject payload = buildGeminiRequestPayload(base64Image);

                // 3. Dispatch HTTP POST to Gemini REST API
                URL url = new URL(GEMINI_API_URL + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(25000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Gemini API Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseStr = readStream(conn.getInputStream());
                    PredictionResult result = parseGeminiResponse(responseStr);
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String errorStr = readStream(conn.getErrorStream());
                    Log.e(TAG, "Gemini API Error (" + responseCode + "): " + errorStr);
                    String userFriendlyMessage = extractUserFriendlyError(responseCode, errorStr);
                    mainHandler.post(() -> callback.onError(userFriendlyMessage));
                }

            } catch (java.net.SocketTimeoutException e) {
                mainHandler.post(() -> callback.onError("Gemini API connection timed out. Please check your internet connection and try again."));
            } catch (java.net.UnknownHostException e) {
                mainHandler.post(() -> callback.onError("No internet connection available to reach Gemini AI Agent."));
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during Gemini analysis", e);
                mainHandler.post(() -> callback.onError("Gemini Vision Agent error: " + e.getMessage()));
            }
        });
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap;
        }

        float ratio = (float) originalWidth / (float) originalHeight;
        int targetWidth;
        int targetHeight;

        if (ratio > 1) {
            targetWidth = maxDimension;
            targetHeight = Math.round(maxDimension / ratio);
        } else {
            targetHeight = maxDimension;
            targetWidth = Math.round(maxDimension * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private JSONObject buildGeminiRequestPayload(String base64Image) throws Exception {
        JSONObject payload = new JSONObject();

        JSONArray contentsArray = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray partsArray = new JSONArray();

        // Prompt part
        JSONObject textPart = new JSONObject();
        String prompt = "You are an expert clinical dietitian and computer vision agent for smart meal planning.\n"
                + "Analyze this meal photograph carefully:\n"
                + "1. Identify the meal or dish name clearly.\n"
                + "2. Estimate the realistic portion weight and serving description (e.g., '1 bowl (~250g)' or '2 slices (~200g)').\n"
                + "3. Predict total calories (kcal).\n"
                + "4. Predict macronutrients: protein in grams, carbohydrates in grams, fat in grams.\n"
                + "5. List visible primary ingredients.\n"
                + "6. Classify dietary type: 'VEGETARIAN', 'NON_VEGETARIAN', or 'VEGAN'.\n"
                + "7. Provide a concise, practical 1-sentence health insight.\n\n"
                + "Respond STRICTLY with a valid JSON object matching this schema:\n"
                + "{\n"
                + "  \"foodName\": \"Full Name of Dish\",\n"
                + "  \"calories\": 520,\n"
                + "  \"protein\": 22.0,\n"
                + "  \"carbs\": 64.0,\n"
                + "  \"fat\": 18.0,\n"
                + "  \"servingSize\": \"2 slices (~200g)\",\n"
                + "  \"confidence\": 0.95,\n"
                + "  \"ingredients\": [\"item 1\", \"item 2\"],\n"
                + "  \"dietaryType\": \"VEGETARIAN\",\n"
                + "  \"healthNotes\": \"Rich in complex carbohydrates and lean protein.\"\n"
                + "}";
        textPart.put("text", prompt);
        partsArray.put(textPart);

        // Image inline data part
        JSONObject imagePart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.put("inlineData", inlineData);
        partsArray.put(imagePart);

        contentObj.put("parts", partsArray);
        contentsArray.put(contentObj);
        payload.put("contents", contentsArray);

        // Generation config for JSON output
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.2);
        payload.put("generationConfig", generationConfig);

        return payload;
    }

    private PredictionResult parseGeminiResponse(String responseJsonStr) throws Exception {
        JSONObject root = new JSONObject(responseJsonStr);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new Exception("Gemini returned empty candidate predictions.");
        }

        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            throw new Exception("Candidate content is missing.");
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            throw new Exception("Candidate response parts are empty.");
        }

        String rawText = parts.getJSONObject(0).optString("text", "{}").trim();
        // Clean markdown block if present
        if (rawText.startsWith("```json")) {
            rawText = rawText.substring(7);
        } else if (rawText.startsWith("```")) {
            rawText = rawText.substring(3);
        }
        if (rawText.endsWith("```")) {
            rawText = rawText.substring(0, rawText.length() - 3);
        }
        rawText = rawText.trim();

        JSONObject resultJson = new JSONObject(rawText);

        String foodName = resultJson.optString("foodName", "Identified Meal");
        int calories = resultJson.optInt("calories", 350);
        float protein = (float) resultJson.optDouble("protein", 10.0);
        float carbs = (float) resultJson.optDouble("carbs", 45.0);
        float fat = (float) resultJson.optDouble("fat", 12.0);
        String servingSize = resultJson.optString("servingSize", "1 Serving (~200g)");
        float confidence = (float) resultJson.optDouble("confidence", 0.95);
        String dietaryType = resultJson.optString("dietaryType", "VEGETARIAN");
        String healthNotes = resultJson.optString("healthNotes", "Analyzed via Gemini Multimodal Vision.");

        List<String> ingredients = new ArrayList<>();
        JSONArray ingArray = resultJson.optJSONArray("ingredients");
        if (ingArray != null) {
            for (int i = 0; i < ingArray.length(); i++) {
                ingredients.add(ingArray.optString(i));
            }
        }

        String label = foodName.toLowerCase().replaceAll("[^a-z0-9]+", "_");

        PredictionResult pred = new PredictionResult(
                label,
                foodName,
                confidence,
                calories,
                protein,
                carbs,
                fat,
                servingSize,
                "gemini_" + label
        );
        pred.setIngredients(ingredients);
        pred.setHealthNotes(healthNotes);
        pred.setDietaryType(dietaryType);
        pred.setAiSource("GEMINI_AGENT");

        return pred;
    }

    private String extractUserFriendlyError(int responseCode, String errorJson) {
        if (responseCode == 400 || responseCode == 403) {
            if (errorJson != null && errorJson.contains("API_KEY_INVALID")) {
                return "Invalid Google Gemini API key. Please check your key at aistudio.google.com";
            }
            return "Gemini API authentication failed (" + responseCode + "). Please re-check your Gemini API key.";
        } else if (responseCode == 429) {
            return "Gemini API rate limit reached. Please wait a few moments and try again.";
        }
        return "Gemini API error (" + responseCode + "): " + (errorJson != null && errorJson.length() < 120 ? errorJson : "Please retry.");
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
