# Naïve Bayes Meal Category Classifier Evaluation Report

## Model Overview
- **Algorithm**: Multinomial Naïve Bayes with Laplace smoothing ($\alpha = 1.0$)
- **Target**: Meal Category (`breakfast`, `lunch`, `snack`, `dinner`)
- **Features**: Dietary preference, Preferred cuisine, Budget, Fitness goal, Activity level, Time of day, Protein preference
- **Train Split**: 64 samples (80%)
- **Test Split**: 16 samples (20%)

## Held-Out Test Set Performance
- **Test Accuracy**: 1.0000 (100.0%)
- **Weighted F1-Score**: 1.0000

### Per-Class Metrics
| Class | Precision | Recall | F1-Score | Support |
|---|---|---|---|---|
| breakfast | 1.000 | 1.000 | 1.000 | 4.0 |
| dinner | 1.000 | 1.000 | 1.000 | 4.0 |
| lunch | 1.000 | 1.000 | 1.000 | 4.0 |
| snack | 1.000 | 1.000 | 1.000 | 4.0 |

### Confusion Matrix
```
Classes: ['breakfast', 'dinner', 'lunch', 'snack']
[[4 0 0 0]
 [0 4 0 0]
 [0 0 4 0]
 [0 0 0 4]]
```

## Mathematical Principle
By Bayes' Theorem under the conditional independence assumption:
$$P(\text{Category} \mid \mathbf{x}) \propto P(\text{Category}) \prod_{j=1}^D P(x_j \mid \text{Category})$$
With Laplace smoothing applied to prevent zero conditional probabilities:
$$P(x_j \mid \text{Category}) = \frac{N_{c, j} + \alpha}{N_c + \alpha D}$$
