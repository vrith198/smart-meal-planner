"""
TensorFlow Lite / LiteRT Export & Quantization Pipeline
Smart Meal Planner AI - On-Device Optimization
"""

import os
import argparse
from pathlib import Path

def export_to_tflite(keras_model_path="ml/models/best_food_model.keras",
                     output_tflite_path="ml/models/food_classifier.tflite",
                     quantize="dynamic_range"):
    """
    Exports a trained Keras MobileNetV2 model to .tflite format with quantization.
    """
    print(f"[INFO] Exporting {keras_model_path} to {output_tflite_path}...")
    try:
        import tensorflow as tf
        model = tf.keras.models.load_model(keras_model_path)
        converter = tf.lite.TFLiteConverter.from_keras_model(model)

        if quantize == "dynamic_range":
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            print("[INFO] Applied dynamic range post-training quantization.")
        elif quantize == "float16":
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_types = [tf.float16]
            print("[INFO] Applied float16 post-training quantization.")

        tflite_model = converter.convert()

        with open(output_tflite_path, "wb") as f:
            f.write(tflite_model)

        size_mb = os.path.getsize(output_tflite_path) / (1024 * 1024)
        print(f"[SUCCESS] Exported TFLite model size: {size_mb:.2f} MB")
        return output_tflite_path
    except ImportError:
        print("[WARN] TensorFlow not available locally. Use Colab notebook to run export.")
        return None

if __name__ == "__main__":
    export_to_tflite()
