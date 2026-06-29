import pandas as pd
from collections import Counter

print("=== Chargement du dataset ===")
df = pd.read_csv("dataset.csv")
print(f"Taille initiale : {df.shape[0]} lignes, {df.shape[1]} colonnes")
print(f"\nColonnes : {list(df.columns)}")

# ── 1. Vérifier les valeurs manquantes ─────────────────────────────────────
print("\n=== 1. Valeurs manquantes ===")
missing = df.isnull().sum()
print(missing[missing > 0] if missing.any() else "✅ Aucune valeur manquante")

df = df.dropna()
print(f"Après suppression NaN : {df.shape[0]} lignes")

# ── 2. Vérifier les types (tout doit être 0/1 sauf label) ──────────────────
print("\n=== 2. Types des colonnes ===")
feature_cols = [c for c in df.columns if c != "label"]
for col in feature_cols:
    unique_vals = df[col].unique()
    if not all(v in [0, 1] for v in unique_vals):
        print(f"⚠️  Colonne '{col}' contient : {unique_vals} → conversion forcée")
        df[col] = df[col].apply(lambda x: 1 if x > 0 else 0)
    else:
        print(f"✅ {col} : OK {unique_vals}")

# ── 3. Supprimer les doublons ───────────────────────────────────────────────
print("\n=== 3. Doublons ===")
duplicates = df.duplicated().sum()
print(f"Doublons trouvés : {duplicates}")
df = df.drop_duplicates()
print(f"Après suppression doublons : {df.shape[0]} lignes")

# ── 4. Distribution des labels ─────────────────────────────────────────────
print("\n=== 4. Distribution des labels ===")
label_counts = Counter(df["label"])
for label, count in sorted(label_counts.items()):
    bar = "█" * count
    print(f"  {label:<12} : {count:>3} {bar}")

min_count = min(label_counts.values())
max_count = max(label_counts.values())
ratio = max_count / min_count
print(f"\nRatio max/min : {ratio:.1f}")
if ratio > 3:
    print("⚠️  Dataset déséquilibré — on va équilibrer par undersampling")
    min_class_size = min_count
    df = df.groupby("label", group_keys=False).apply(
        lambda x: x.sample(min(len(x), min_class_size * 2), random_state=42)
    )
    print(f"Après équilibrage : {df.shape[0]} lignes")
    print(f"Nouvelle distribution : {Counter(df['label'])}")
else:
    print("✅ Distribution acceptable")

# ── 5. Vérifier les lignes avec que des 0 (CV vide) ───────────────────────
print("\n=== 5. Lignes avec zéro compétence ===")
zero_rows = df[df[feature_cols].sum(axis=1) == 0]
print(f"Lignes vides trouvées : {len(zero_rows)}")
if len(zero_rows) > 0:
    print("→ Suppression des lignes vides")
    df = df[df[feature_cols].sum(axis=1) > 0]

# ── 6. Vérifier les colonnes inutiles (toujours 0) ────────────────────────
print("\n=== 6. Colonnes constantes (toujours 0) ===")
const_cols = [col for col in feature_cols if df[col].nunique() == 1]
if const_cols:
    print(f"⚠️  Colonnes inutiles : {const_cols} → suppression")
    df = df.drop(columns=const_cols)
else:
    print("✅ Aucune colonne constante")

# ── 7. Shuffle final ───────────────────────────────────────────────────────
df = df.sample(frac=1, random_state=42).reset_index(drop=True)

# ── 8. Sauvegarder ────────────────────────────────────────────────────────
df.to_csv("dataset_clean.csv", index=False)
print(f"\n✅ dataset_clean.csv sauvegardé — {df.shape[0]} lignes propres")
print(f"Shape finale : {df.shape}")