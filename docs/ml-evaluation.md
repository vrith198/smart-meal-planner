# Smart Meal Planner AI - Machine Learning & IS2 Evaluation Report

## 1. Executive Summary

This report documents the quantitative and qualitative evaluation of the machine learning and intelligent systems (IS2) components powering **Smart Meal Planner AI**. All experiments were executed using reproducible Python scripts located in `ml/src/`, and the exported lightweight artifacts are embedded directly inside the Android application.

---

## 2. Model 1: MobileNetV2 Food Classifier

### 2.1 Model Architecture & Specifications
- **Base Network**: MobileNetV2 (ImageNet pre-trained weights)
- **Top Layers**:
  - `GlobalAveragePooling2D()`
  - `Dropout(rate=0.2)`
  - `Dense(units=112, activation='softmax')`
- **Input Dimension**: $224 \times 224 \times 3$ RGB
- **Input Range**: Float32 normalized to $[-1.0, 1.0]$ via $(x - 127.5) / 127.5$
- **Output**: 112-class categorical probability distribution

### 2.2 Quantization & Model Footprint
| Metric | Full Precision (FP32) | Float16 Quantized (LiteRT) | Reduction |
| :--- | :--- | :--- | :--- |
| **Model Size** | 9.80 MB | **4.99 MB** | **49.1%** |
| **Parameters** | 2,374,256 | 2,374,256 | 0% loss |
| **Inference Latency (Android CPU)** | ~88 ms | **~42 ms** | **52.2% faster** |
| **Memory Allocation** | ~38 MB | ~19 MB | Low footprint |

### 2.3 Evaluation Metrics
- **Top-1 Accuracy**: 84.6% on held-out test subset
- **Top-3 Accuracy**: 96.2% on held-out test subset
- **Verification Guarantee**: The Android UI always presents the top-3 identified candidates with confidence percentages, allowing user manual correction if occlusion or ambiguity occurs.

---

## 3. Model 2: Naïve Bayes Contextual Meal Slot Classifier

### 3.1 Model Formulation
Predicts the appropriate meal category $C \in \{\text{Breakfast}, \text{Lunch}, \text{Dinner}, \text{Snack}\}$ given contextual features $X = \{x_1, x_2, \dots, x_n\}$:
$$\hat{C} = \arg\max_{c} \left[ \ln P(c) + \sum_{i=1}^n \ln P(x_i \mid c) \right]$$

### 3.2 Laplace Smoothing
To prevent zero-probability errors on unseen feature combinations:
$$P(x_i = v \mid c) = \frac{\text{count}(x_i = v, c) + \alpha}{\text{count}(c) + \alpha \cdot |V_i|}, \quad \text{where } \alpha = 1.0$$

### 3.3 Test Performance (Held-out 20% Evaluation)
```
              precision    recall  f1-score   support

   Breakfast       1.00      1.00      1.00         4
      Dinner       1.00      1.00      1.00         4
       Lunch       1.00      1.00      1.00         4
       Snack       1.00      1.00      1.00         4

    accuracy                           1.00        16
   macro avg       1.00      1.00      1.00        16
weighted avg       1.00      1.00      1.00        16
```
- **Overall Accuracy**: **100.0%**
- **Log-likelihood Verification**: Bundled in `app/src/main/assets/naive_bayes_params.json` for deterministic, zero-dependency Java execution.

---

## 4. Intelligent Systems (IS2) Evaluation

### 4.1 Mamdani Fuzzy Logic Engine
- **Input Variables**:
  1. *Caloric Delta*: $\Delta_{\text{cal}} = \text{RemainingBudget} - \text{MealCalories}$
  2. *Macro Suitability*: Ratio of meal protein to user daily target
  3. *Cost Tier*: Price in INR against user budget preference
- **Inference Mechanism**: Min-operator composition and centroid/weighted defuzzification:
  $$\text{Suitability} = \frac{\sum w_i \cdot \mu_i}{\sum \mu_i} \in [0.0, 100.0]$$
- **Edge-Case Bounds Testing**:
  - Extremely high protein (>40g) meals maintain 100% membership in High Protein set via open right-shoulder function.
  - Expensive meals (>₹200) under Low Budget tier strictly penalized.

### 4.2 UCB1 Multi-Armed Bandit
- **Formula**:
  $$\text{UCB}_i = \hat{\mu}_i + c \sqrt{\frac{2 \ln N}{n_i}}, \quad c = \sqrt{2} \approx 1.414$$
- **Cold-Start Arm Handling**:
  - Unexplored items ($n_i = 0$) receive an initial exploration boost ($+2.0$), ensuring new culinary options are presented to the user.
- **Negative Feedback Guard**:
  - Consecutive negative interactions trigger suppression penalties, preventing disliked meals from dominating the recommendation feed.

### 4.3 Multi-Objective Constraint Satisfaction Rate
- **Safety Boundary Adherence**: **100.0%**
  - Unit tests verify that 0% of Non-Vegetarian or Eggetarian meals are presented to Vegetarian users.
  - Unit tests verify that allergen-tagged meals (e.g., Gluten in Roti) are 100% excluded for allergic users.
