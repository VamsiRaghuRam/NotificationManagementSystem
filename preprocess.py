"""
SmartNotify - Phase 2 Data Preprocessing & ML Dataset Preparation Pipeline
Author: Antigravity AI
Project: SmartNotify - ML-Based Intelligent Notification Management System

This script performs reproducible data loading, key normalization, deduplication, 
text cleaning, target priority mapping (LOW, MEDIUM, HIGH), feature construction, 
and stratified 80/20 train/test splitting without introducing data leakage.
"""

import os
import json
import re
import pandas as pd
from sklearn.model_selection import train_test_split


def run_preprocessing_pipeline(
    raw_filepath: str = r'c:\Users\2005k\OneDrive\Documents\Vamsi\Temp\Notifications\training_data.jsonl',
    output_dir: str = 'data/processed',
    random_state: int = 42,
    test_size: float = 0.20
):
    print("=" * 60)
    print("SMARTNOTIFY - PHASE 2 DATA PREPROCESSING PIPELINE")
    print("=" * 60)

    # Ensure output directories exist
    os.makedirs(output_dir, exist_ok=True)
    os.makedirs('data/reports', exist_ok=True)

    # 1. Load Raw Dataset
    print(f"\n[Step 1] Loading raw dataset from: {raw_filepath}")
    with open(raw_filepath, 'r', encoding='utf-8') as f:
        raw_records = [json.loads(line) for line in f]
    
    total_raw_count = len(raw_records)
    print(f"-> Loaded {total_raw_count} raw JSON records.")

    # 2. Key Normalization & Missing Value Handling
    print("\n[Step 2] Normalizing key typos and inspecting missing values...")
    cleaned_records = []
    key_typos_fixed = 0
    missing_target_count = 0

    for r in raw_records:
        n = r.get('notification', {})
        c = r.get('classification', {})

        raw_id = r.get('id', '')
        app = n.get('app', 'unknown')

        # Fix minor key typos for app display name
        app_disp = n.get('app_display_name')
        if not app_disp:
            for alt_key in ['app_disable_name', 'app_displayname', 'app_delivery', 'app_deliver', 'app_covered']:
                if alt_key in n:
                    app_disp = n[alt_key]
                    key_typos_fixed += 1
                    break
        if not app_disp:
            app_disp = app.split('.')[-1].capitalize()

        title = str(n.get('title', '')).strip()
        body = str(n.get('body', '')).strip()
        folder = str(c.get('folder', 'Alerts')).strip()
        raw_p = c.get('priority')

        # Check for missing priority target
        if raw_p is None:
            missing_target_count += 1
            continue

        cleaned_records.append({
            'id': raw_id,
            'app': app,
            'app_display_name': app_disp,
            'title': title,
            'body': body,
            'folder': folder,
            'priority_raw': int(raw_p)
        })

    print(f"-> Display name key typos normalized: {key_typos_fixed}")
    print(f"-> Records removed due to missing target: {missing_target_count}")

    df = pd.DataFrame(cleaned_records)

    # 3. Duplicate Removal
    print("\n[Step 3] Identifying and removing duplicate records...")
    df['dedup_key'] = df['app'] + '||' + df['title'] + '||' + df['body']
    duplicates_count = df.duplicated(subset=['dedup_key']).sum()
    df_clean = df.drop_duplicates(subset=['dedup_key']).copy()
    df_clean.drop(columns=['dedup_key'], inplace=True)
    
    cleaned_count = len(df_clean)
    print(f"-> Exact duplicates removed: {duplicates_count}")
    print(f"-> Clean records remaining: {cleaned_count}")

    # 4. Text Normalization & Field Combination
    print("\n[Step 4] Preprocessing text fields and constructing combined NLP feature...")
    
    def clean_text(text: str) -> str:
        text = str(text)
        # Collapse multi-spaces without removing punctuation or case tokens
        text = re.sub(r'\s+', ' ', text).strip()
        return text

    df_clean['title_clean'] = df_clean['title'].apply(clean_text)
    df_clean['body_clean'] = df_clean['body'].apply(clean_text)

    # Combined text feature for TF-IDF pipeline
    df_clean['combined_text'] = (
        df_clean['app_display_name'] + ' ' +
        df_clean['title_clean'] + ' ' +
        df_clean['body_clean'] + ' ' +
        df_clean['folder']
    ).apply(clean_text)

    # 5. Target Priority Normalization
    print("\n[Step 5] Mapping integer priority (1-5) to SmartNotify 3-Tier target (LOW, MEDIUM, HIGH)...")
    
    def map_priority(p: int) -> str:
        if p in [1, 2]:
            return 'LOW'
        elif p == 3:
            return 'MEDIUM'
        elif p in [4, 5]:
            return 'HIGH'
        return 'UNKNOWN'

    df_clean['priority_target'] = df_clean['priority_raw'].apply(map_priority)
    
    class_counts = df_clean['priority_target'].value_counts()
    class_props = df_clean['priority_target'].value_counts(normalize=True) * 100
    
    for c in ['LOW', 'MEDIUM', 'HIGH']:
        print(f"   - {c:7s}: {class_counts[c]:5d} samples ({class_props[c]:.2f}%)")

    # Save Cleaned Full Dataset
    cleaned_csv_path = os.path.join(output_dir, 'cleaned_dataset.csv')
    df_clean.to_csv(cleaned_csv_path, index=False, encoding='utf-8')
    print(f"-> Saved full cleaned dataset to: {cleaned_csv_path}")

    # 6. Stratified Train/Test Split
    print(f"\n[Step 6] Performing Stratified Train/Test Split (test_size={test_size}, random_state={random_state})...")
    train_df, test_df = train_test_split(
        df_clean,
        test_size=test_size,
        random_state=random_state,
        stratify=df_clean['priority_target']
    )

    train_csv_path = os.path.join(output_dir, 'train.csv')
    test_csv_path = os.path.join(output_dir, 'test.csv')

    train_df.to_csv(train_csv_path, index=False, encoding='utf-8')
    test_df.to_csv(test_csv_path, index=False, encoding='utf-8')

    print(f"-> Saved Train Dataset ({len(train_df)} rows) to: {train_csv_path}")
    print(f"-> Saved Test Dataset  ({len(test_df)} rows) to: {test_csv_path}")

    print("\n" + "=" * 60)
    print("PHASE 2 PREPROCESSING COMPLETED SUCCESSFULLY")
    print("=" * 60)
    
    return {
        'total_raw': total_raw_count,
        'duplicates_removed': duplicates_count,
        'clean_records': cleaned_count,
        'train_records': len(train_df),
        'test_records': len(test_df),
        'class_counts': class_counts.to_dict()
    }


if __name__ == '__main__':
    run_preprocessing_pipeline()
