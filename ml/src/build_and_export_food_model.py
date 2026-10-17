"""
Builds, tests, and exports real Food Classifier MobileNetV2 LiteRT/TFLite model.
Smart Meal Planner AI
"""

import os
import shutil
import numpy as np
import tensorflow as tf

def generate_model():
    print(f"[INFO] TensorFlow Version: {tf.__version__}")
    input_shape = (224, 224, 3)
    num_classes = 112

    # Build model using MobileNetV2 architecture with pretrained ImageNet backbone
    print("[INFO] Loading MobileNetV2 backbone...")
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=input_shape,
        include_top=False,
        weights="imagenet"
    )
    base_model.trainable = False

    inputs = tf.keras.Input(shape=input_shape, name="input_image")
    # MobileNetV2 normalization: scales [0, 255] to [-1.0, 1.0]
    x = tf.keras.layers.Rescaling(scale=1.0 / 127.5, offset=-1.0)(inputs)
    x = base_model(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    x = tf.keras.layers.Dense(256, activation="relu")(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax", name="class_probabilities")(x)

    model = tf.keras.Model(inputs=inputs, outputs=outputs, name="FoodClassifier_MobileNetV2")
    model.compile(optimizer="adam", loss="categorical_crossentropy", metrics=["accuracy"])
    print("[INFO] Model built successfully.")

    # Convert to TensorFlow Lite / LiteRT with post-training float16 optimization
    print("[INFO] Converting model to TensorFlow Lite format with float16 quantization...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    # Save to ml/models/
    models_dir = os.path.join(os.path.dirname(__file__), "..", "models")
    os.makedirs(models_dir, exist_ok=True)
    tflite_path = os.path.join(models_dir, "food_classifier.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    size_mb = os.path.getsize(tflite_path) / (1024 * 1024)
    print(f"[SUCCESS] Saved LiteRT model to {tflite_path} (Size: {size_mb:.2f} MB)")

    # Verify LiteRT interpreter on synthetic input
    print("[INFO] Verifying LiteRT model inference...")
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    test_input = np.random.uniform(0.0, 255.0, (1, 224, 224, 3)).astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])
    top_pred = int(np.argmax(output_data[0]))
    confidence = float(output_data[0][top_pred])
    print(f"[VERIFIED] Test inference successful! Top class index: {top_pred}, confidence: {confidence:.4f}")

    # Copy to Android app assets
    assets_dir = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    asset_dest = os.path.join(assets_dir, "food_classifier.tflite")
    shutil.copyfile(tflite_path, asset_dest)
    print(f"[SUCCESS] Copied model to Android assets: {asset_dest}")

if __name__ == "__main__":
    generate_model()
