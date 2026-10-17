"""
Naïve Bayes Meal Category Classifier Training & Evaluation Pipeline
Smart Meal Planner AI - Intelligent Systems (IS2) Module
"""

import os
import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score
from sklearn.preprocessing import OneHotEncoder
from sklearn.naive_bayes import MultinomialNB, CategoricalNB
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score, f1_score
import joblib

def train_and_evaluate():
    # 1. Load Dataset
    data_path = os.path.join(os.path.dirname(__file__), "..", "data", "meal_preferences.csv")
    df = pd.read_csv(data_path)
    print(f"Loaded dataset: {df.shape[0]} rows, {df.shape[1]} columns")

    feature_cols = [
        "dietary_pref", "cuisine_pref", "budget_category", 
        "fitness_goal", "activity_level", "time_of_day", "protein_pref"
    ]
    target_col = "target_meal_category"

    X = df[feature_cols]
    y = df[target_col]

    # 2. Train / Test Split (80% Train, 20% Held-out Test)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )
    print(f"Train split: {len(X_train)} samples, Test split: {len(X_test)} samples")

    # 3. Encoding
    encoder = OneHotEncoder(handle_unknown='ignore', sparse_output=False)
    X_train_encoded = encoder.fit_transform(X_train)
    X_test_encoded = encoder.transform(X_test)

    # 4. Fit Multinomial Naive Bayes with Laplace Smoothing (alpha=1.0)
    alpha = 1.0
    clf = MultinomialNB(alpha=alpha)
    clf.fit(X_train_encoded, y_train)

    # 5. Evaluate on Held-out Test Set
    y_pred = clf.predict(X_test_encoded)
    acc = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average='weighted')
    cm = confusion_matrix(y_test, y_pred, labels=clf.classes_)
    report = classification_report(y_test, y_pred, target_names=clf.classes_, output_dict=True)

    print("\n--- Model Evaluation Results ---")
    print(f"Accuracy: {acc:.4f}")
    print(f"F1-Score (weighted): {f1:.4f}")
    print("\nConfusion Matrix:")
    print(cm)
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=clf.classes_))

    # 6. Save Model Artifacts
    models_dir = os.path.join(os.path.dirname(__file__), "..", "models")
    reports_dir = os.path.join(os.path.dirname(__file__), "..", "reports")
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(reports_dir, exist_ok=True)

    joblib.dump(clf, os.path.join(models_dir, "naive_bayes_model.pkl"))
    joblib.dump(encoder, os.path.join(models_dir, "one_hot_encoder.pkl"))

    # Export conditional probabilities and class priors for embedded Java implementation
    feature_names = list(encoder.get_feature_names_out(feature_cols))
    model_params = {
        "classes": list(clf.classes_),
        "class_priors": {c: float(np.exp(clf.class_log_prior_[i])) for i, c in enumerate(clf.classes_)},
        "features": feature_names,
        "feature_prob": {}
    }
    for c_idx, c_name in enumerate(clf.classes_):
        model_params["feature_prob"][c_name] = {
            feature_names[f_idx]: float(np.exp(clf.feature_log_prob_[c_idx][f_idx]))
            for f_idx in range(len(feature_names))
        }

    with open(os.path.join(models_dir, "naive_bayes_params.json"), "w") as f:
        json.dump(model_params, f, indent=2)

    # Also save to app assets for runtime Java engine
    assets_dir = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    with open(os.path.join(assets_dir, "naive_bayes_params.json"), "w") as f:
        json.dump(model_params, f, indent=2)

    # 7. Write Evaluation Report
    report_md = f"""# Naïve Bayes Meal Category Classifier Evaluation Report

## Model Overview
- **Algorithm**: Multinomial Naïve Bayes with Laplace smoothing ($\\alpha = {alpha}$)
- **Target**: Meal Category (`breakfast`, `lunch`, `snack`, `dinner`)
- **Features**: Dietary preference, Preferred cuisine, Budget, Fitness goal, Activity level, Time of day, Protein preference
- **Train Split**: {len(X_train)} samples (80%)
- **Test Split**: {len(X_test)} samples (20%)

## Held-Out Test Set Performance
- **Test Accuracy**: {acc:.4f} ({acc*100:.1f}%)
- **Weighted F1-Score**: {f1:.4f}

### Per-Class Metrics
| Class | Precision | Recall | F1-Score | Support |
|---|---|---|---|---|
"""
    for cls in clf.classes_:
        c_rep = report[cls]
        report_md += f"| {cls} | {c_rep['precision']:.3f} | {c_rep['recall']:.3f} | {c_rep['f1-score']:.3f} | {c_rep['support']} |\n"

    report_md += f"""
### Confusion Matrix
```
Classes: {list(clf.classes_)}
{cm}
```

## Mathematical Principle
By Bayes' Theorem under the conditional independence assumption:
$$P(\\text{{Category}} \\mid \\mathbf{{x}}) \\propto P(\\text{{Category}}) \\prod_{{j=1}}^D P(x_j \\mid \\text{{Category}})$$
With Laplace smoothing applied to prevent zero conditional probabilities:
$$P(x_j \\mid \\text{{Category}}) = \\frac{{N_{{c, j}} + \\alpha}}{{N_c + \\alpha D}}$$
"""
    with open(os.path.join(reports_dir, "naive_bayes_evaluation.md"), "w") as f:
        f.write(report_md)

    print("Artifacts and reports successfully generated!")

if __name__ == "__main__":
    train_and_evaluate()
