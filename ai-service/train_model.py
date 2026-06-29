import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, confusion_matrix
import joblib
import numpy as np

print("=== Chargement dataset nettoyé ===")
df = pd.read_csv("dataset_clean.csv")   # ← dataset_clean pas dataset
print(f"Shape : {df.shape}")

X = df.drop("label", axis=1)
y = df["label"]

print(f"Features : {list(X.columns)}")
print(f"Classes  : {y.unique()}")

# ── Split ──────────────────────────────────────────────────────────────────
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y  # stratify = garder proportion
)
print(f"\nTrain : {len(X_train)} | Test : {len(X_test)}")

# ── Entraînement ───────────────────────────────────────────────────────────
model = RandomForestClassifier(
    n_estimators=200,
    max_depth=10,
    min_samples_split=2,
    random_state=42,
    class_weight="balanced"  # gère le déséquilibre résiduel
)
model.fit(X_train, y_train)

# ── Évaluation ─────────────────────────────────────────────────────────────
print("\n=== Rapport de classification ===")
y_pred = model.predict(X_test)
print(classification_report(y_test, y_pred))

print("=== Matrice de confusion ===")
cm = confusion_matrix(y_test, y_pred, labels=model.classes_)
print(pd.DataFrame(cm, index=model.classes_, columns=model.classes_))

print("\n=== Cross-validation (5 folds) ===")
cv_scores = cross_val_score(model, X, y, cv=5, scoring="accuracy")
print(f"Scores : {np.round(cv_scores, 3)}")
print(f"Moyenne : {cv_scores.mean():.3f} ± {cv_scores.std():.3f}")

# ── Feature importance ─────────────────────────────────────────────────────
print("\n=== Importance des features ===")
importances = pd.Series(model.feature_importances_, index=X.columns)
print(importances.sort_values(ascending=False).to_string())

# ── Sauvegarde ─────────────────────────────────────────────────────────────
joblib.dump(model, "model.joblib")
print("\n✅ model.joblib sauvegardé")