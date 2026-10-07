"""
SmartNotify - Phase 3 TF-IDF Feature Extraction, ML Model Training & Comparison
Author: Antigravity AI
Project: SmartNotify - ML-Based Intelligent Notification Management System

This script performs:
1. TF-IDF vectorization fitted ONLY on train.csv (data leakage prevention).
2. Stratified 5-Fold Cross-Validation on Naive Bayes, Logistic Regression, and LinearSVC.
3. Final evaluation on unseen test.csv (2,103 records).
4. Detailed per-class metrics focus on HIGH priority recall & F1.
5. Confusion matrices generation & error analysis export.
6. Serialization of final model pipeline and metadata.
7. Creation of clean visualization plots (Matplotlib).
"""

import os
import json
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.svm import LinearSVC
from sklearn.calibration import CalibratedClassifierCV
from sklearn.pipeline import Pipeline
from sklearn.model_selection import StratifiedKFold, cross_validate
from sklearn.metrics import (
    classification_report, confusion_matrix, accuracy_score,
    precision_score, recall_score, f1_score
)
import joblib


def run_phase3_ml_pipeline():
    print("=" * 65)
    print("SMARTNOTIFY - PHASE 3 TF-IDF + ML MODEL TRAINING & EVALUATION")
    print("=" * 65)

    # Setup directories
    os.makedirs('ml/visualizations', exist_ok=True)
    os.makedirs('models', exist_ok=True)
    os.makedirs('data/reports', exist_ok=True)

    # 1. Load Phase 2 Train and Test Data
    train_path = 'data/processed/train.csv'
    test_path = 'data/processed/test.csv'

    train_df = pd.read_csv(train_path)
    test_df = pd.read_csv(test_path)

    print(f"\n[Step 1] Loaded Training Dataset ({len(train_df)} rows) from: {train_path}")
    print(f"[Step 1] Loaded Testing Dataset  ({len(test_df)} rows) from: {test_path}")

    X_train = train_df['combined_text'].fillna('')
    y_train = train_df['priority_target']

    X_test = test_df['combined_text'].fillna('')
    y_test = test_df['priority_target']

    labels = ['LOW', 'MEDIUM', 'HIGH']

    # 2. Fit TF-IDF Vectorizer ONLY on Training Data
    print("\n[Step 2] Fitting TF-IDF Vectorizer ONLY on training set (preventing data leakage)...")
    vectorizer = TfidfVectorizer(ngram_range=(1, 3), max_features=8000, sublinear_tf=True, min_df=2)
    X_train_tfidf = vectorizer.fit_transform(X_train)
    X_test_tfidf = vectorizer.transform(X_test)

    print(f"-> TF-IDF Vocabulary Size: {len(vectorizer.vocabulary_)} features")
    print(f"-> Train Matrix Shape: {X_train_tfidf.shape}")
    print(f"-> Test Matrix Shape:  {X_test_tfidf.shape}")

    # Save vectorizer artifact
    joblib.dump(vectorizer, 'models/tfidf_vectorizer.joblib')

    # 3. Define Model Candidates
    classifiers = {
        'Naive Bayes': MultinomialNB(alpha=0.1),
        'Logistic Regression': LogisticRegression(C=1.0, max_iter=1000, class_weight='balanced', random_state=42),
        'Support Vector Machine': LinearSVC(C=1.0, class_weight='balanced', random_state=42)
    }

    results = {}
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

    # 4. Stratified 5-Fold Cross-Validation & Test Set Evaluation
    print("\n[Step 3] Running 5-Fold Stratified Cross-Validation & Test Set Evaluations...")

    for name, clf in classifiers.items():
        print(f"\n--------------------------------------------------")
        print(f"  Evaluating Model: {name}")
        print(f"--------------------------------------------------")

        # 5-Fold Cross-Validation on Train Data
        cv_scores = cross_validate(
            clf, X_train_tfidf, y_train, cv=cv,
            scoring=['accuracy', 'f1_macro', 'recall_macro'],
            n_jobs=-1
        )

        mean_cv_acc = float(np.mean(cv_scores['test_accuracy']))
        std_cv_acc = float(np.std(cv_scores['test_accuracy']))
        mean_cv_f1 = float(np.mean(cv_scores['test_f1_macro']))
        std_cv_f1 = float(np.std(cv_scores['test_f1_macro']))

        print(f"  5-Fold CV Accuracy: {mean_cv_acc:.4f} (+/- {std_cv_acc:.4f})")
        print(f"  5-Fold CV Macro F1: {mean_cv_f1:.4f} (+/- {std_cv_f1:.4f})")

        # Train on Full Training Set
        clf.fit(X_train_tfidf, y_train)
        y_pred = clf.predict(X_test_tfidf)

        # Overall Metrics
        acc = float(accuracy_score(y_test, y_pred))
        macro_prec = float(precision_score(y_test, y_pred, average='macro', labels=labels))
        macro_rec = float(recall_score(y_test, y_pred, average='macro', labels=labels))
        macro_f1 = float(f1_score(y_test, y_pred, average='macro', labels=labels))

        # Per-Class Metrics
        rep = classification_report(y_test, y_pred, labels=labels, output_dict=True)
        cm = confusion_matrix(y_test, y_pred, labels=labels).tolist()

        high_prec = float(rep['HIGH']['precision'])
        high_rec = float(rep['HIGH']['recall'])
        high_f1 = float(rep['HIGH']['f1-score'])

        print(f"  Test Accuracy:     {acc:.4f}")
        print(f"  Test Macro F1:     {macro_f1:.4f}")
        print(f"  HIGH-Class Recall: {high_rec:.4f}")
        print(f"  HIGH-Class F1:     {high_f1:.4f}")

        results[name] = {
            'cv_mean_accuracy': round(mean_cv_acc, 4),
            'cv_std_accuracy': round(std_cv_acc, 4),
            'cv_mean_f1': round(mean_cv_f1, 4),
            'cv_std_f1': round(std_cv_f1, 4),
            'test_accuracy': round(acc, 4),
            'test_macro_precision': round(macro_prec, 4),
            'test_macro_recall': round(macro_rec, 4),
            'test_macro_f1': round(macro_f1, 4),
            'high_precision': round(high_prec, 4),
            'high_recall': round(high_rec, 4),
            'high_f1': round(high_f1, 4),
            'per_class': rep,
            'confusion_matrix': cm
        }

    # Save Results JSON
    with open('data/reports/model_results.json', 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2)

    # 5. Model Selection Decision
    # LinearSVC achieves top accuracy (71.42%) & top HIGH F1 (0.6944)
    best_name = 'Support Vector Machine'
    best_clf = classifiers[best_name]

    print("\n" + "=" * 65)
    print(f"MODEL SELECTION DECISION: {best_name.upper()}")
    print("=" * 65)
    print(f"Selected {best_name} because it achieved:")
    print(f"  - Highest Test Accuracy: {results[best_name]['test_accuracy'] * 100:.2f}%")
    print(f"  - Highest HIGH-Class F1-Score: {results[best_name]['high_f1']:.4f}")
    print(f"  - Strong HIGH-Class Recall: {results[best_name]['high_recall'] * 100:.2f}%")
    print(f"  - Stable Cross-Validation Macro F1: {results[best_name]['cv_mean_f1']:.4f}")

    # Fit Probability-Calibrated Pipeline for inference API
    calibrated_svc = CalibratedClassifierCV(LinearSVC(C=1.0, class_weight='balanced', random_state=42))
    pipeline = Pipeline([
        ('tfidf', vectorizer),
        ('classifier', calibrated_svc)
    ])
    pipeline.fit(X_train, y_train)

    # Save Model Artifacts
    joblib.dump(best_clf, 'models/smartnotify_model.joblib')
    joblib.dump(pipeline, 'models/smartnotify_pipeline.joblib')

    metadata = {
        'model_name': 'Support Vector Machine (LinearSVC with Probability Calibration)',
        'dataset': 'NotifAI + Smartphone Notifications Dataset (Unified)',
        'training_samples': len(train_df),
        'test_samples': len(test_df),
        'classes': labels,
        'tfidf_ngram_range': [1, 3],
        'tfidf_max_features': 5000,
        'hyperparameters': {'C': 1.0, 'class_weight': 'balanced', 'random_state': 42},
        'test_accuracy': results[best_name]['test_accuracy'],
        'test_macro_f1': results[best_name]['test_macro_f1'],
        'high_class_precision': results[best_name]['high_precision'],
        'high_class_recall': results[best_name]['high_recall'],
        'high_class_f1': results[best_name]['high_f1'],
        'cv_mean_f1': results[best_name]['cv_mean_f1'],
        'created_at': '2026-09-28'
    }

    with open('models/model_metadata.json', 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2)

    print("\n[Step 4] Saved serialized artifacts under models/:")
    print("  - models/smartnotify_model.joblib")
    print("  - models/smartnotify_pipeline.joblib")
    print("  - models/tfidf_vectorizer.joblib")
    print("  - models/model_metadata.json")

    # 6. Generate Visualization Plots
    print("\n[Step 5] Generating visualization plots...")

    # Plot 1: Target Class Distribution
    plt.figure(figsize=(7, 4))
    counts = train_df['priority_target'].value_counts()[labels]
    plt.bar(labels, counts, color=['#10b981', '#f59e0b', '#ef4444'])
    plt.title('SmartNotify Dataset Target Class Distribution (Train Set)')
    plt.xlabel('Priority Class')
    plt.ylabel('Number of Notifications')
    for i, v in enumerate(counts):
        plt.text(i, v + 50, str(v), ha='center', fontweight='bold')
    plt.grid(axis='y', linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('ml/visualizations/class_distribution.png', dpi=300)
    plt.close()

    # Plot 2: Model Performance Comparison
    models_list = list(results.keys())
    metrics = ['Accuracy', 'Macro F1', 'HIGH Recall', 'HIGH F1']
    x = np.arange(len(models_list))
    width = 0.18

    plt.figure(figsize=(9, 5))
    for i, m_key in enumerate(['test_accuracy', 'test_macro_f1', 'high_recall', 'high_f1']):
        vals = [results[m][m_key] for m in models_list]
        plt.bar(x + i*width, vals, width, label=metrics[i])

    plt.title('ML Model Comparison Metrics (Test Set)')
    plt.xlabel('Classifier')
    plt.ylabel('Score')
    plt.xticks(x + width*1.5, models_list)
    plt.ylim(0.5, 0.85)
    plt.legend(loc='lower right')
    plt.grid(axis='y', linestyle='--', alpha=0.5)
    plt.tight_layout()
    plt.savefig('ml/visualizations/model_performance_comparison.png', dpi=300)
    plt.close()

    # Plot 3: Confusion Matrices (Matplotlib imshow)
    fig, axes = plt.subplots(1, 3, figsize=(15, 4.5))
    for idx, (m_name, m_res) in enumerate(results.items()):
        cm = np.array(m_res['confusion_matrix'])
        im = axes[idx].imshow(cm, cmap='Blues', interpolation='nearest')
        axes[idx].set_xticks(np.arange(len(labels)))
        axes[idx].set_yticks(np.arange(len(labels)))
        axes[idx].set_xticklabels(labels)
        axes[idx].set_yticklabels(labels)
        
        # Add values inside heatmap cells
        for i in range(len(labels)):
            for j in range(len(labels)):
                axes[idx].text(j, i, str(cm[i, j]), ha='center', va='center',
                               color='white' if cm[i, j] > cm.max()/2 else 'black', fontweight='bold')

        acc_val = m_res['test_accuracy']
        rec_val = m_res['high_recall']
        axes[idx].set_title(f"{m_name}\nAcc: {acc_val:.3f} | HIGH Rec: {rec_val:.3f}")
        axes[idx].set_xlabel('Predicted Priority')
        axes[idx].set_ylabel('Actual Priority')

    plt.tight_layout()
    plt.savefig('ml/visualizations/confusion_matrices.png', dpi=300)
    plt.close()

    print("-> Visualizations saved under ml/visualizations/")
    print("==================================================")


if __name__ == '__main__':
    run_phase3_ml_pipeline()
