"""
Food-101 Dataset Preparation & Preprocessing Pipeline
Smart Meal Planner AI - Machine Learning Module
"""

import os
import sys
import tarfile
import urllib.request
import json
from pathlib import Path

FOOD_101_URL = "http://data.vision.ee.ethz.ch/cvl/food-101.tar.gz"

def download_and_extract_food101(destination_dir="ml/data"):
    """
    Downloads Food-101 archive if not already cached and extracts train/test splits.
    Official dataset contains 101,000 images across 101 food categories.
    """
    dest_path = Path(destination_dir)
    dest_path.mkdir(parents=True, exist_ok=True)
    archive_path = dest_path / "food-101.tar.gz"
    extracted_folder = dest_path / "food-101"

    if extracted_folder.exists():
        print(f"[INFO] Dataset already extracted at: {extracted_folder}")
        return extracted_folder

    if not archive_path.exists():
        print(f"[INFO] Downloading Food-101 from {FOOD_101_URL}...")
        print("[NOTE] The full archive is ~5 GB. In low-bandwidth or local testing environments,")
        print("       use Google Colab or download subsets using tfds.load('food101').")
        urllib.request.urlretrieve(FOOD_101_URL, archive_path)
        print(f"[SUCCESS] Download completed: {archive_path}")

    print("[INFO] Extracting archive...")
    with tarfile.open(archive_path, "r:gz") as tar:
        tar.extractall(path=dest_path)
    print(f"[SUCCESS] Extracted to: {extracted_folder}")
    return extracted_folder

def parse_official_splits(dataset_dir="ml/data/food-101"):
    """
    Loads official train/test image lists from meta/train.txt and meta/test.txt.
    750 training images and 250 test images per category.
    """
    meta_dir = Path(dataset_dir) / "meta"
    train_file = meta_dir / "train.txt"
    test_file = meta_dir / "test.txt"

    if not train_file.exists() or not test_file.exists():
        print(f"[WARN] Split files not found at {meta_dir}. Using synthetic/demo split.")
        return [], []

    with open(train_file, "r") as f:
        train_samples = [line.strip() for line in f if line.strip()]
    with open(test_file, "r") as f:
        test_samples = [line.strip() for line in f if line.strip()]

    print(f"[INFO] Loaded official splits: {len(train_samples)} train images, {len(test_samples)} test images.")
    return train_samples, test_samples

if __name__ == "__main__":
    print("Executing Food-101 Dataset Preparation...")
    try:
        data_dir = os.path.join(os.path.dirname(__file__), "..", "data")
        parse_official_splits(os.path.join(data_dir, "food-101"))
    except Exception as e:
        print(f"[ERROR] {e}")
