"""
SmartNotify - Phase 3 Error Analysis & Detailed Model Evaluation Script
Author: Antigravity AI
"""

import sys
import json
import pandas as pd
import joblib

sys.stdout.reconfigure(encoding='utf-8')


def run_error_analysis():
    test_df = pd.read_csv('data/processed/test.csv')
    pipeline = joblib.load('models/smartnotify_pipeline.joblib')

    X_test = test_df['combined_text'].fillna('')
    y_test = test_df['priority_target']

    y_pred = pipeline.predict(X_test)
    test_df['predicted'] = y_pred

    errors = test_df[test_df['priority_target'] != test_df['predicted']]
    print(f"Total Test Set Samples: {len(test_df)}")
    print(f"Total Prediction Errors: {len(errors)} ({len(errors)/len(test_df)*100:.2f}%)")

    high_as_low = test_df[(test_df['priority_target'] == 'HIGH') & (test_df['predicted'] == 'LOW')]
    low_as_high = test_df[(test_df['priority_target'] == 'LOW') & (test_df['predicted'] == 'HIGH')]
    med_as_low = test_df[(test_df['priority_target'] == 'MEDIUM') & (test_df['predicted'] == 'LOW')]
    med_as_high = test_df[(test_df['priority_target'] == 'MEDIUM') & (test_df['predicted'] == 'HIGH')]

    print(f"\nBreakdown of Error Categories:")
    print(f"  1. HIGH predicted as LOW (Critical False Negatives): {len(high_as_low)}")
    print(f"  2. LOW predicted as HIGH (False Alarm Interruptions): {len(low_as_high)}")
    print(f"  3. MEDIUM predicted as LOW:  {len(med_as_low)}")
    print(f"  4. MEDIUM predicted as HIGH: {len(med_as_high)}")

    print("\n--- SAMPLE HIGH AS LOW ERRORS (Critical False Negatives) ---")
    for i, (_, row) in enumerate(high_as_low.head(5).iterrows(), 1):
        print(f"  Sample {i}: [{row['app_display_name']}] '{row['title']}' -> '{row['body']}'")

    print("\n--- SAMPLE LOW AS HIGH ERRORS (False Alarms) ---")
    for i, (_, row) in enumerate(low_as_high.head(5).iterrows(), 1):
        print(f"  Sample {i}: [{row['app_display_name']}] '{row['title']}' -> '{row['body']}'")

    # Export errors to CSV for review
    errors.to_csv('data/reports/model_errors.csv', index=False)
    print("\nSaved detailed prediction errors to data/reports/model_errors.csv")


if __name__ == '__main__':
    run_error_analysis()
