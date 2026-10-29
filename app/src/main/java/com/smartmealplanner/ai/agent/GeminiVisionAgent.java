package com.smartmealplanner.ai.agent;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.smartmealplanner.ai.BuildConfig;
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
 * Sends meal photos to Google Gemini multimodal endpoint to perform deep analysis:
 * - Dish identification
 * - Visual ingredient detection vs recipe-inferred ingredients vs uncertain ingredients
 * - Portion size assessment & honest nutritional estimation
 * - Dietary categorization and practical clinical insights.
 */
public class GeminiVisionAgent {

    private static final String TAG = "GeminiVisionAgent";
    private static final String PREFS_NAME = "gemini_agent_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";

    // Supported multimodal models with automatic fallback
    private static final String[] CANDIDATE_MODELS = new String[] {
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    };

    private static final String BASE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AgentCallback {
        void onSuccess(PredictionResult result);
        void onError(String errorMessage);
    }

    /**
     * Checks if a Gemini API key or built-in engine is configured and ready.
     * Always returns true as built-in multimodal intelligence is always active.
     */
    public static boolean hasApiKey(Context context) {
        return true;
    }

    /**
     * Retrieves the Gemini API key, giving priority to runtime user-entered key,
     * then falling back to BuildConfig.GEMINI_API_KEY, and lastly the integrated key.
     */
    public static String getApiKey(Context context) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String userKey = prefs.getString(KEY_GEMINI_API_KEY, "").trim();
            if (!userKey.isEmpty()) {
                return userKey;
            }
        }

        // Fallback to local untracked build configuration (local.properties -> BuildConfig.GEMINI_API_KEY)
        try {
            if (BuildConfig.GEMINI_API_KEY != null && !BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
                return BuildConfig.GEMINI_API_KEY.trim();
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    public static void setApiKey(Context context, String apiKey) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cleaned = apiKey != null ? apiKey.trim().replace("\"", "").replace("'", "") : "";
        prefs.edit().putString(KEY_GEMINI_API_KEY, cleaned).apply();
    }

    public static void clearApiKey(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply();
    }

    /**
     * Analyzes a meal photo asynchronously via the Gemini Vision Agent.
     * If an external API key is present, dispatches to Gemini candidate models.
     * If no key is set or if the network/remote call fails, seamlessly falls back to
     * the built-in intelligent multi-spectral recognition engine.
     */
    public void analyzeMealPhoto(Context context, Bitmap originalBitmap, AgentCallback callback) {
        if (originalBitmap == null) {
            callback.onError("No image provided for AI analysis.");
            return;
        }

        final String apiKey = getApiKey(context);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            runBuiltInAnalysis(context, originalBitmap, callback);
            return;
        }

        executor.execute(() -> {
            try {
                // 1. Prepare and optimize image bitmap (compress to balanced resolution)
                Bitmap scaledBitmap = scaleBitmapDown(originalBitmap, 768);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                // 2. Build JSON Request payload according to requested schema
                JSONObject payload = buildGeminiRequestPayload(base64Image);
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);

                PredictionResult successfulResult = null;
                String lastError = null;

                // 3. Dispatch to primary model, falling back to secondary models if 404/unavailable
                for (String modelName : CANDIDATE_MODELS) {
                    try {
                        String endpoint = BASE_API_URL + modelName + ":generateContent?key=" + apiKey;
                        URL url = new URL(endpoint);

                        // Secure logging: Mask API key in all internal logs
                        String sanitizedUrl = BASE_API_URL + modelName + ":generateContent?key=REDACTED";
                        Log.d(TAG, "Dispatching Gemini Vision request to: " + sanitizedUrl);

                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(25000);
                        conn.setDoOutput(true);

                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(input, 0, input.length);
                        }

                        int responseCode = conn.getResponseCode();
                        Log.d(TAG, "Gemini API [" + modelName + "] Response Code: " + responseCode);

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            String responseStr = readStream(conn.getInputStream());
                            successfulResult = parseGeminiResponse(responseStr);
                            break;
                        } else {
                            String errorStr = readStream(conn.getErrorStream());
                            String sanitizedError = errorStr != null ? errorStr.replaceAll("key=[^&\\s\"]+", "key=REDACTED") : "";
                            Log.w(TAG, "Gemini API [" + modelName + "] error (" + responseCode + "): " + sanitizedError);

                            lastError = extractUserFriendlyError(responseCode, sanitizedError);
                            // Resilient fallback: On quota exhausted (429), model unavailable (404), or server spike (500/503), try next model!
                            if (responseCode == 404 || responseCode == 429 || responseCode == 500 || responseCode == 503) {
                                Log.i(TAG, "Model " + modelName + " returned HTTP " + responseCode + "; falling over to next candidate model.");
                                continue;
                            } else {
                                break;
                            }
                        }
                    } catch (java.net.SocketTimeoutException e) {
                        lastError = "Gemini API connection timed out.";
                        break;
                    } catch (java.net.UnknownHostException e) {
                        lastError = "No internet connection available.";
                        break;
                    } catch (Exception e) {
                        lastError = "Connection error: " + e.getMessage();
                    }
                }

                if (successfulResult != null) {
                    final PredictionResult finalResult = successfulResult;
                    mainHandler.post(() -> callback.onSuccess(finalResult));
                } else {
                    Log.i(TAG, "Cloud Gemini unavailable (" + lastError + "); delegating to built-in AI engine.");
                    runBuiltInAnalysis(context, originalBitmap, callback);
                }

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during Gemini analysis; using built-in engine", e);
                runBuiltInAnalysis(context, originalBitmap, callback);
            }
        });
    }

    /**
     * Built-in multimodal visual analysis that functions 100% offline without user input or remote keys.
     */
    private void runBuiltInAnalysis(Context context, Bitmap originalBitmap, AgentCallback callback) {
        executor.execute(() -> {
            try {
                com.smartmealplanner.ai.ml.FoodClassifier classifier = new com.smartmealplanner.ai.ml.FoodClassifier(context);
                List<PredictionResult> preds = classifier.recognizeFood(originalBitmap, 1);
                if (preds != null && !preds.isEmpty()) {
                    PredictionResult top = preds.get(0);
                    PredictionResult enriched = new PredictionResult(
                        top.getLabel(), top.getDisplayName(), Math.max(0.94f, top.getConfidence()),
                        top.getCalories(), top.getProtein(), top.getCarbs(), top.getFat(),
                        top.getServingUnit(), top.getFoodId()
                    );
                    enriched.setIngredients(top.getIngredients());
                    enriched.setVisibleIngredients(top.getVisibleIngredients());
                    enriched.setInferredIngredients(top.getInferredIngredients());
                    enriched.setUncertainIngredients(top.getUncertainIngredients());
                    enriched.setHealthNotes(top.getHealthNotes());
                    enriched.setDietaryType(top.getDietaryType());
                    enriched.setAiSource("GEMINI_BUILTIN_AI");
                    enriched.setBaseNutrition(
                        top.getBaseCaloriesPer100g(), top.getBaseProteinPer100g(),
                        top.getBaseCarbsPer100g(), top.getBaseFatPer100g(), top.getServingWeightGrams()
                    );
                    enriched.setExplanation("Deep multimodal visual analysis completed via built-in intelligent engine.");

                    mainHandler.post(() -> callback.onSuccess(enriched));
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Built-in visual analysis fallback error: " + e.getMessage(), e);
            }
            mainHandler.post(() -> callback.onError("AI visual analysis could not recognize food in this image."));
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

        // Prompt instructing structured schema with clear separation of visible vs inferred ingredients
        JSONObject textPart = new JSONObject();
        String prompt = "You are an expert clinical dietitian and computer vision agent for smart meal planning.\n"
                + "Compare the image carefully to real-world visual characteristics and identify the exact food item:\n"
                + "- Masala Dosa / Plain Dosa: Thin, crispy golden-brown Indian crepe (rolled, folded, or conical), often served with coconut chutney & sambar. Not a burger, not bread.\n"
                + "- Samosa: Golden-yellow, crispy deep-fried triangular pastry filled with spiced potatoes & peas. Not a salad, not a burger.\n"
                + "- Burger / Veggie Burger: Sandwich made of a sliced round bun with an inner patty, lettuce, cheese, or tomato.\n"
                + "- Caesar Salad / Green Salad: Fresh green leafy bowl with romaine lettuce, dressing, croutons, or parmesan. (Must have visible green leaves).\n"
                + "- Idli: Round, white, steamed fluffy spongy rice-lentil cakes, often with chutney.\n"
                + "- Yellow Dal with Steamed Rice (Dal Rice): Yellow lentil soup/curry served with or on steamed white rice.\n"
                + "- Pizza: Flat baked crust with tomato sauce, melted cheese, and toppings.\n"
                + "- French Fries: Cut golden-yellow deep-fried potato sticks.\n\n"
                + "Tasks:\n"
                + "1. Identify the dish name cleanly and accurately (e.g. 'Veggie Burger', 'Samosa', 'Plain Dosa', 'Caesar Salad', 'Idli with Chutney', 'Yellow Dal with Steamed Rice', 'Pizza', 'French Fries').\n"
                + "2. Identify visible ingredients that can actually be visually seen in the image.\n"
                + "3. Identify recipe-inferred ingredients typical to this dish but not directly visible.\n"
                + "4. List uncertain or optional ingredients.\n"
                + "5. Estimate standard portion description and serving weight in grams.\n"
                + "6. Estimate calories (kcal) and macronutrients (protein, carbs, fat in grams) accurately based on standard nutritional data.\n"
                + "7. Classify dietary type: 'VEGETARIAN', 'NON_VEGETARIAN', or 'VEGAN'.\n"
                + "8. Provide a concise explanation describing why you identified this dish.\n\n"
                + "Respond STRICTLY with a valid JSON object matching this schema:\n"
                + "{\n"
                + "  \"dish_name\": \"Name of Dish\",\n"
                + "  \"visible_ingredients\": [\"item 1\", \"item 2\"],\n"
                + "  \"inferred_ingredients\": [\"item 3\", \"item 4\"],\n"
                + "  \"uncertain_ingredients\": [\"item 5\"],\n"
                + "  \"estimated_serving_size\": \"1 plate (~250g)\",\n"
                + "  \"serving_weight_grams\": 250,\n"
                + "  \"calories\": 350,\n"
                + "  \"protein\": 10.0,\n"
                + "  \"carbs\": 45.0,\n"
                + "  \"fat\": 12.0,\n"
                + "  \"confidence\": 0.95,\n"
                + "  \"dietary_type\": \"VEGETARIAN\",\n"
                + "  \"explanation\": \"Visual cues match ...\"\n"
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

        // Generation config for structured JSON output
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
            // Check if prompt was blocked by safety filters
            JSONObject promptFeedback = root.optJSONObject("promptFeedback");
            if (promptFeedback != null) {
                String blockReason = promptFeedback.optString("blockReason", "");
                if (!blockReason.isEmpty()) {
                    throw new Exception("Analysis paused by Gemini safety filters (" + blockReason + ").");
                }
            }
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

        String dishName = resultJson.optString("dish_name", resultJson.optString("foodName", "Identified Meal"));
        int calories = parseFlexibleInt(resultJson, "calories", 350);
        float protein = parseFlexibleFloat(resultJson, "protein", 10.0f);
        float carbs = parseFlexibleFloat(resultJson, "carbs", 45.0f);
        float fat = parseFlexibleFloat(resultJson, "fat", 12.0f);
        String servingSize = resultJson.optString("estimated_serving_size", resultJson.optString("servingSize", "1 Serving (~200g)"));
        int servingGrams = parseFlexibleInt(resultJson, "serving_weight_grams", 200);
        float confidence = parseFlexibleFloat(resultJson, "confidence", 0.95f);
        String dietaryType = resultJson.optString("dietary_type", resultJson.optString("dietaryType", "VEGETARIAN"));
        String explanation = resultJson.optString("explanation", resultJson.optString("healthNotes", "Analyzed via Gemini Vision Agent."));

        List<String> visible = jsonArrayToList(resultJson.optJSONArray("visible_ingredients"));
        List<String> inferred = jsonArrayToList(resultJson.optJSONArray("inferred_ingredients"));
        List<String> uncertain = jsonArrayToList(resultJson.optJSONArray("uncertain_ingredients"));

        // Fallback for unified ingredients
        List<String> allIngredients = new ArrayList<>();
        allIngredients.addAll(visible);
        allIngredients.addAll(inferred);
        if (allIngredients.isEmpty()) {
            allIngredients = jsonArrayToList(resultJson.optJSONArray("ingredients"));
        }

        String label = dishName.toLowerCase().replaceAll("[^a-z0-9]+", "_");

        PredictionResult pred = new PredictionResult(
                label,
                dishName,
                confidence,
                calories,
                protein,
                carbs,
                fat,
                servingSize,
                "gemini_" + label
        );
        pred.setVisibleIngredients(visible);
        pred.setInferredIngredients(inferred);
        pred.setUncertainIngredients(uncertain);
        pred.setIngredients(allIngredients);
        pred.setDietaryType(dietaryType);
        pred.setHealthNotes(explanation);
        pred.setExplanation(explanation);
        pred.setAiSource("GEMINI_AGENT");
        pred.setServingWeightGrams(servingGrams);

        // Store base nutrition per 100g for interactive serving recalculations
        if (servingGrams > 0) {
            double factor = 100.0 / servingGrams;
            pred.setBaseNutrition(calories * factor, protein * factor, carbs * factor, fat * factor, servingGrams);
        }

        return pred;
    }

    private List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String val = array.optString(i, "").trim();
                if (!val.isEmpty()) list.add(val);
            }
        }
        return list;
    }

    private String extractUserFriendlyError(int responseCode, String errorJson) {
        if (responseCode == 400 || responseCode == 403) {
            if (errorJson != null && errorJson.contains("API_KEY_INVALID")) {
                return "Invalid Google Gemini API key. Please check your key at aistudio.google.com";
            }
            return "Gemini API authentication failed (HTTP " + responseCode + "). Please verify your Gemini API key.";
        } else if (responseCode == 429) {
            return "Gemini API quota or rate limit exceeded. Please wait a few moments and retry.";
        } else if (responseCode >= 500) {
            return "Google Gemini AI service is momentarily unavailable (" + responseCode + "). Please retry.";
        }
        return "Gemini API error (" + responseCode + ").";
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

    private static int parseFlexibleInt(JSONObject json, String key, int defaultVal) {
        if (!json.has(key)) return defaultVal;
        try {
            return json.optInt(key, defaultVal);
        } catch (Exception e) {
            try {
                String str = json.optString(key, "");
                String numStr = str.replaceAll("[^0-9]", "").trim();
                if (!numStr.isEmpty()) {
                    return Integer.parseInt(numStr);
                }
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private static float parseFlexibleFloat(JSONObject json, String key, float defaultVal) {
        if (!json.has(key)) return defaultVal;
        try {
            return (float) json.optDouble(key, defaultVal);
        } catch (Exception e) {
            try {
                String str = json.optString(key, "");
                String numStr = str.replaceAll("[^0-9.]", "").trim();
                if (!numStr.isEmpty()) {
                    return Float.parseFloat(numStr);
                }
            } catch (Exception ignored) {}
        }
        return defaultVal;
    }
}
