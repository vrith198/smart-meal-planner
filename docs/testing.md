# Smart Meal Planner AI - Automated Testing Suite & Verification

## 1. Testing Strategy & Philosophy

Safety, deterministic nutritional calculations, and algorithmic robustness are critical in health and wellness applications. Smart Meal Planner AI uses a rigorous testing suite covering:
- Exact clinical calorie and macronutrient scaling.
- Boundary adherence and defuzzification bounds in Mamdani Fuzzy Logic.
- Mathematical exploration bounds in UCB1 Multi-Armed Bandit.
- Hard dietary and allergen safety filtering.
- Zero-frequency Laplace smoothing in Naïve Bayes classification.

---

## 2. Test Suite Execution & Summary

### 2.1 Execution Command
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.1"
.\gradlew.bat testDebugUnitTest
```

### 2.2 Results Matrix
| Test Suite Class | Tests Executed | Passed | Failed | Skipped | Status |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `NutritionCalculatorTest` | 8 | 8 | 0 | 0 | **PASSED** |
| `FuzzyLogicEngineTest` | 6 | 6 | 0 | 0 | **PASSED** |
| `UCB1RecommenderTest` | 5 | 5 | 0 | 0 | **PASSED** |
| `MealRankerTest` | 5 | 5 | 0 | 0 | **PASSED** |
| `NaiveBayesMealClassifierTest` | 3 | 3 | 0 | 0 | **PASSED** |
| **Total** | **27** | **27** | **0** | **0** | **100% PASS** |

---

## 3. Detailed Test Catalog

### 3.1 `NutritionCalculatorTest`
1. `testMifflinStJeorBmr_Male`: Validates male BMR formula:
   $$\text{BMR} = 10 \cdot \text{weight} + 6.25 \cdot \text{height} - 5 \cdot \text{age} + 5$$
2. `testMifflinStJeorBmr_Female`: Validates female BMR formula:
   $$\text{BMR} = 10 \cdot \text{weight} + 6.25 \cdot \text{height} - 5 \cdot \text{age} - 161$$
3. `testTdeeCalculation_ActivityMultipliers`: Validates activity level multipliers:
   - Sedentary: 1.2
   - Light: 1.375
   - Moderate: 1.55
   - Very Active: 1.725
   - Extra Active: 1.9
4. `testGoalCalorieAdjustment`: Verifies deficit/surplus offsets:
   - Weight Loss: $-500$ kcal
   - Muscle Gain: $+400$ kcal
   - Maintenance: $+0$ kcal
5. `testMacroDistributionCalculation`: Validates caloric density constants (Protein: 4 kcal/g, Carbs: 4 kcal/g, Fat: 9 kcal/g).
6. `testNutrientPer100gCalculation_ExactScaling`: Ensures linear scaling from 100g base to custom serving weight.
7. `testNutrientPer100gCalculation_ZeroAndNegativeGuard`: Ensures zero or negative weights do not produce NaN or runtime exceptions.
8. `testResolveServingWeight_AllOptions`: Verifies mapping of Small, Medium, Large, and Custom serving sizes against food item weights.

### 3.2 `FuzzyLogicEngineTest`
1. `testTriangularMembershipFunction`: Validates $\mu(x)$ triangle slopes and peak coordinates.
2. `testTrapezoidalMembershipFunction`: Validates trapezoid flat core $[b, c]$ where $\mu(x) = 1.0$.
3. `testSuitabilityScore_HighSuitabilityMeal`: Confirms that a meal matching calorie delta, high protein, and low budget achieves a suitability score $\ge 75.0$.
4. `testSuitabilityScore_LowBudgetPenaltyForExpensiveMeal`: Asserts that expensive meals (>₹200) receive significant penalties when user budget tier is `LOW`.
5. `testSuitabilityScore_MuscleGainProteinBoost`: Validates protein suitability weighting for muscle building goals.
6. `testSuitabilityScore_StrictBoundsGuarantee`: Enforces that suitability scores never exceed $[0.0, 100.0]$ across edge cases.

### 3.3 `UCB1RecommenderTest`
1. `testColdStartArm_ReceivesExplorationPriority`: Asserts that an arm with $n_i = 0$ pulls receives high exploration priority over frequently pulled arms.
2. `testExploitationVsExplorationBalance`: Confirms the logarithmic exploration decay as $n_i \to \infty$.
3. `testFeedbackRewardMapping`: Asserts discrete reward assignments:
   - Thumbs Up = $1.0$
   - Thumbs Down = $0.0$
   - Eat Action = $0.8$
   - Skip Action = $0.2$
4. `testRewardUpdateMechanism`: Tests incremental update of cumulative reward and pull count in Room database entities.
5. `testDislikeSuppressionGuard`: Verifies that consecutive dislikes significantly penalize meal ranking.

### 3.4 `MealRankerTest`
1. `testHardDietaryRestriction_VegetarianUserNeverGetsNonVegOrEggetarian`: Guarantees 0% leakage of non-vegetarian items into a vegetarian user's recommendation pool.
2. `testHardDietaryRestriction_VeganUserOnlyGetsVegan`: Guarantees 100% adherence to strict vegan constraints (excludes dairy, egg, meat).
3. `testAllergenFiltering_GlutenAllergyExcludesRoti`: Verifies that gluten-containing items (e.g., wheat roti) are strictly excluded when the user profile contains a gluten allergy.
4. `testVarietyPenaltyAppliedToRecentlyLoggedMeals`: Confirms meals logged in the last 24 hours receive a variety penalty ($-20.0$) to avoid repetitive menu fatigue.
5. `testExplainableAIProducesGroundedReasons`: Asserts that every recommended candidate is accompanied by specific, verifiable explanation pills.

### 3.5 `NaiveBayesMealClassifierTest`
1. `testPredictMealCategory_ValidCategory`: Verifies morning queries map to `BREAKFAST`, evening to `DINNER` or `SNACK`.
2. `testProbabilitiesSumToOne`: Asserts that posterior probabilities normalize to $1.0 \pm 10^{-5}$.
3. `testLaplaceSmoothing_HandlesUnseenOrNullFeaturesWithoutCrashing`: Verifies that null, empty, or novel categories default safely via Laplace smoothing without NaN or crash.
