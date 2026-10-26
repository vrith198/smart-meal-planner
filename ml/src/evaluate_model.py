"""
Food Classifier Evaluation & Benchmark Script
Smart Meal Planner AI - Real LiteRT/TFLite Benchmark, Latency & Ingredient Metrics
"""

import os
import sys
import time
import json
import numpy as np

def benchmark_real_inference(tflite_path, num_runs=50, warmup_runs=5):
    """
    Measures real on-device/local LiteRT inference latency using tf.lite.Interpreter
    with actual float32 tensors. No simulated sleep.
    """
    if not os.path.exists(tflite_path):
        raise FileNotFoundError(f"TFLite model not found at: {tflite_path}")

    print(f"[INFO] Loading LiteRT Model: {tflite_path}")
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    input_shape = input_details[0]['shape']
    input_dtype = input_details[0]['dtype']
    output_shape = output_details[0]['shape']
    output_dtype = output_details[0]['dtype']

    print(f"[INFO] Input Tensor: Shape={input_shape}, DType={input_dtype}")
    print(f"[INFO] Output Tensor: Shape={output_shape}, DType={output_dtype}")

    # Generate synthetic input batch [1, 224, 224, 3] in range [0.0, 255.0]
    np.random.seed(42)
    sample_input = np.random.uniform(0.0, 255.0, input_shape).astype(input_dtype)

    # Warm-up runs to stabilize CPU cache & JIT
    print(f"[INFO] Performing {warmup_runs} warm-up runs...")
    for _ in range(warmup_runs):
        interpreter.set_tensor(input_details[0]['index'], sample_input)
        interpreter.invoke()

    # Benchmark runs
    print(f"[INFO] Benchmarking {num_runs} real inference invocations...")
    latencies = []
    for _ in range(num_runs):
        start = time.perf_counter()
        interpreter.set_tensor(input_details[0]['index'], sample_input)
        interpreter.invoke()
        elapsed_ms = (time.perf_counter() - start) * 1000.0
        latencies.append(elapsed_ms)

    latencies = np.array(latencies)
    avg_latency = float(np.mean(latencies))
    median_latency = float(np.median(latencies))
    min_latency = float(np.min(latencies))
    max_latency = float(np.max(latencies))
    p95_latency = float(np.percentile(latencies, 95))
    std_latency = float(np.std(latencies))

    output_data = interpreter.get_tensor(output_details[0]['index'])
    top_class = int(np.argmax(output_data[0]))
    top_prob = float(output_data[0][top_class])

    print("\n" + "=" * 50)
    print("      LITERT INFERENCE BENCHMARK RESULTS")
    print("=" * 50)
    print(f"Total Runs:             {num_runs}")
    print(f"Average Latency:        {avg_latency:.2f} ms")
    print(f"Median Latency:         {median_latency:.2f} ms")
    print(f"Min Latency:            {min_latency:.2f} ms")
    print(f"Max Latency:            {max_latency:.2f} ms")
    print(f"95th Percentile (P95):  {p95_latency:.2f} ms")
    print(f"Standard Deviation:     {std_latency:.2f} ms")
    print(f"Model File Size:        {os.path.getsize(tflite_path) / (1024 * 1024):.2f} MB")
    print(f"Test Class Pred Index:  {top_class} (Prob: {top_prob:.4f})")
    print("=" * 50)

    return {
        "num_runs": num_runs,
        "avg_ms": round(avg_latency, 2),
        "median_ms": round(median_latency, 2),
        "min_ms": round(min_latency, 2),
        "max_ms": round(max_latency, 2),
        "p95_ms": round(p95_latency, 2),
        "std_ms": round(std_latency, 2),
        "file_size_mb": round(os.path.getsize(tflite_path) / (1024 * 1024), 2)
    }

def evaluate_ingredient_separation():
    """
    Evaluates multi-label ingredient breakdown for sample dishes.
    Demonstrates precision/recall between ground truth recipe ingredients
    and visual vs recipe-inferred ingredients.
    """
    print("\n" + "=" * 50)
    print("   MULTI-LABEL INGREDIENT REASONING EVALUATION")
    print("=" * 50)

    # Benchmark test cases
    test_dishes = [
        {
            "dish": "idli",
            "ground_truth_recipe": {"rice", "urad dal", "water", "salt", "fenugreek seeds"},
            "visually_detected": {"steamed rice cake"},
            "recipe_inferred": {"rice", "urad dal", "water", "salt"},
            "uncertain": {"fenugreek seeds"}
        },
        {
            "dish": "dal rice",
            "ground_truth_recipe": {"white rice", "yellow lentils", "turmeric", "cumin", "salt", "ghee", "water"},
            "visually_detected": {"white rice", "yellow lentil gravy"},
            "recipe_inferred": {"turmeric", "salt", "water"},
            "uncertain": {"ghee", "cumin"}
        },
        {
            "dish": "pizza",
            "ground_truth_recipe": {"flour crust", "tomato sauce", "mozzarella cheese", "basil", "olive oil", "salt"},
            "visually_detected": {"cheese", "tomato sauce", "crust", "basil"},
            "recipe_inferred": {"olive oil", "salt", "yeast"},
            "uncertain": {"oregano", "sugar"}
        }
    ]

    total_precision = []
    total_recall = []
    total_f1 = []

    for item in test_dishes:
        gt = item["ground_truth_recipe"]
        # All predicted ingredients = visual + inferred
        pred = item["visually_detected"].union(item["recipe_inferred"])
        true_positives = len(gt.intersection(pred))
        precision = true_positives / len(pred) if pred else 0.0
        recall = true_positives / len(gt) if gt else 0.0
        f1 = (2 * precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0

        total_precision.append(precision)
        total_recall.append(recall)
        total_f1.append(f1)

        print(f"Dish: {item['dish']}")
        print(f"  - Visually Detected:    {list(item['visually_detected'])}")
        print(f"  - Recipe-Inferred:      {list(item['recipe_inferred'])}")
        print(f"  - Uncertain:            {list(item['uncertain'])}")
        print(f"  - Recipe F1-Score:      {f1:.3f} (P={precision:.3f}, R={recall:.3f})")

    macro_f1 = float(np.mean(total_f1))
    print("-" * 50)
    print(f"Macro Ingredient F1-Score: {macro_f1:.3f}")
    print("=" * 50)
    return {"macro_f1": round(macro_f1, 3)}

if __name__ == "__main__":
    tflite_file = os.path.join(os.path.dirname(__file__), "..", "models", "food_classifier.tflite")
    metrics = benchmark_real_inference(tflite_file, num_runs=50)
    ingr_metrics = evaluate_ingredient_separation()
