# Smart Meal Planner AI - Datasets & Provenance

## 1. Overview of Data Architecture

Nutritional recommendation and food recognition systems require verified, scientifically grounded datasets to ensure user safety and clinical accuracy. Smart Meal Planner AI integrates five dedicated datasets across computer vision, clinical nutrition tables, and contextual machine learning.

---

## 2. Dataset Descriptions

### Dataset A: Food-101 (Main Food Image Classifier)
- **Official Source**: Computer Vision Lab, ETH Zurich ([Food-101 Link](https://data.vision.ee.ethz.ch/cvl/datasets_extra/food-101/))
- **TensorFlow Catalog**: `tensorflow_datasets:food101`
- **Scale**: 101,000 images across 101 food categories (75,750 train, 25,250 test).
- **Purpose**: Trains the on-device convolutional vision model (`food_classifier.tflite`).
- **Processing & Preprocessing**:
  - Resized to $224 \times 224 \times 3$ RGB.
  - Normalization: $\frac{x - 127.5}{127.5} \in [-1.0, 1.0]$.
  - Data Augmentation: Random horizontal flip, rotation ($\pm 15^\circ$), zoom ($\pm 10\%$).
- **Quantization & Footprint**:
  - Unquantized FP32: ~9.8 MB
  - Exported Float16 Quantized: **4.99 MB** (optimized for mobile execution).
- **Provenance Note**: Food-101 is strictly an image classification dataset. It contains no direct caloric labels and cannot be used directly for direct pixel-to-calorie regression.

---

### Dataset B: USDA FoodData Central (Standard Reference)
- **Official Portal**: [USDA FoodData Central](https://fdc.nal.usda.gov/)
- **Purpose**: Ground truth nutritional profile for standardized global food items.
- **Normalization Standard**: All entries normalized per 100g of edible weight:
  - Energy (kcal)
  - Protein (g)
  - Total Carbohydrates (g)
  - Total Fat (g)
  - Dietary Fiber (g)

---

### Dataset C: Curated Indian Food Nutrition Database (IFCT 2017)
- **Location**: `ml/data/indian_food_nutrition.csv` and `nutrition-data/verified_foods.json`
- **Bundled Android Asset**: `app/src/main/assets/food_nutrition_db.json`
- **Provenance**: Indian Food Composition Tables (IFCT 2017, National Institute of Nutrition, ICMR) supplemented with verified USDA records.
- **Catalog Size**: 18 curated staple items:
  1. Plain Roti (Whole Wheat Chapati)
  2. Steamed White Rice
  3. Yellow Moong Dal Tadka
  4. Paneer Butter Masala
  5. Masala Dosa
  6. Idli (Fermented Rice-Lentil Cake)
  7. Vegetable Poha
  8. Vegetable Upma
  9. Chole (Chickpea Curry)
  10. Rajma (Red Kidney Bean Curry)
  11. Vegetable Biryani
  12. Palak Paneer
  13. Boiled Egg
  14. Chicken Curry
  15. Curd (Plain Dahi)
  16. Samosa
  17. Gulab Jamun
  18. Mixed Green Salad
- **Serving Granularity**: Exact weights for Small, Medium, and Large portions.
- **Commercial & Safety Attributes**:
  - Allergen tags: Gluten, Dairy, Egg, etc.
  - Ingredient lists for consolidated grocery generation.
  - Estimated realistic cost in Indian Rupees (₹) for budget optimization.

---

### Dataset D: Nutrition5k (Exploratory / Reference)
- **Official Repository**: [google-research-datasets/Nutrition5k](https://github.com/google-research-datasets/Nutrition5k)
- **Scale**: ~5,000 real-world meal plates with overhead RGB-D scanning and chemical mass spectrometry nutrition data.
- **Architectural Decision**: While Nutrition5k provides direct plate-level calories, training an end-to-end RGB regression network leads to severe generalization failure across different plates, angles, and lighting conditions. Smart Meal Planner AI instead adopts the industry standard **Two-Stage Calibrated Pipeline**:
  1. Stage 1: Robust categorical identification via MobileNetV2.
  2. Stage 2: Volumetric serving selection (Small / Medium / Large / Custom Weight in grams) mapped against verified IFCT/USDA reference tables.

---

### Dataset E: Naïve Bayes Contextual Meal Slot Dataset
- **Location**: `ml/data/meal_preferences.csv`
- **Bundled Android Asset**: `app/src/main/assets/naive_bayes_params.json`
- **Dataset Structure**: 80 labeled context vectors containing:
  - `dietary_type`: Vegetarian, Non-Vegetarian, Vegan, Eggetarian
  - `time_of_day`: Morning, Afternoon, Evening, Night
  - `fitness_goal`: Weight Loss, Muscle Gain, Maintenance, Budget Friendly
  - `activity_level`: Sedentary, Moderate, Active
  - `budget_tier`: Low, Medium, High
  - `target_slot`: Breakfast, Lunch, Snack, Dinner
- **Model Parameters**: Prior class probabilities and conditional likelihood matrices with Laplace smoothing ($k=1.0$) stored in JSON for offline inference in pure Java.
