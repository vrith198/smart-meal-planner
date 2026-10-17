# Food-101 Classifier Evaluation & Benchmark Report

## 1. Model Overview & Specifications
- **Architecture**: MobileNetV2 with Transfer Learning (ImageNet pre-trained feature extractor)
- **Classification Head**: GlobalAveragePooling2D + BatchNorm + Dropout(0.2) + Dense(256, ReLU) + Dense(112, Softmax)
- **Input Dimension**: `1 x 224 x 224 x 3` (Float32)
- **Input Normalization**: MobileNetV2 scale: `(pixel / 127.5) - 1.0` mapping `[0, 255]` to `[-1.0, 1.0]`
- **Output Dimension**: `1 x 112` (probabilities over 112 food classes)
- **Target Runtime**: Android LiteRT / TensorFlow Lite 2.14+
- **Export Format**: FlatBuffers `.tflite`
- **Quantization**: Post-training float16 optimization
- **Model File Size**: 4.99 MB (reduced from ~14 MB uncompressed Float32)

## 2. Dataset Split & Methodology
- **Source**: Food-101 official dataset + curated regional food records
- **Split Strategy**: 750 train images and 250 test images per standard class
- **Augmentation**: Random horizontal flip, random rotation ($\pm 15^\circ$), random zoom ($\pm 10\%$), color jitter
- **Validation Split**: 20% hold-out from training set for early stopping

## 3. Evaluation Metrics on Held-Out Test Set
| Metric | Performance |
|---|---|
| **Top-1 Accuracy** | **81.4%** |
| **Top-5 Accuracy** | **94.2%** |
| **Macro Precision** | **0.819** |
| **Macro Recall** | **0.812** |
| **Macro F1-Score** | **0.815** |

## 4. On-Device Latency Benchmarks
Benchmarked across 50 consecutive runs on standard mobile CPU/NNAPI:
- **Average Inference Latency**: `18.5 ms`
- **95th Percentile Latency (P95)**: `24.1 ms`
- **Memory Footprint at Runtime**: `~12.8 MB RAM`
- **Hardware Acceleration**: Compatible with Android NNAPI and GPU delegate

## 5. Scope & Limitations
- **Food-101 is a classification dataset**: It trains categorical identification of foods from photographs. It does **not** contain ground-truth calorie labels or food weight measurements.
- **Serving Weight**: Food weight is determined in the app through user-selected standard portions (Small, Medium, Large) or exact Custom Grams, multiplied with verified USDA/IFCT nutritional composition data.
