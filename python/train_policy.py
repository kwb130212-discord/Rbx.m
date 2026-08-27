"""Small offline trainer for the Android policy model.

Input CSV columns: action,reward
This is intentionally independent of game networking and trains only a local
policy from user-provided telemetry.
"""
from __future__ import annotations
import csv
import json
import sys
from collections import defaultdict

DEFAULT = {"attack": 0.5, "dodge": 0.5, "move": 0.4, "explore": 0.2}


def train(path: str) -> dict[str, float]:
    values = DEFAULT.copy()
    counts = defaultdict(int)
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            action = row.get("action", "").strip()
            if action not in values:
                continue
            try:
                reward = float(row["reward"])
            except (KeyError, TypeError, ValueError):
                continue
            counts[action] += 1
            alpha = 1.0 / counts[action]
            values[action] += alpha * (reward - values[action])
    return values


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: python train_policy.py telemetry.csv")
    print(json.dumps(train(sys.argv[1]), ensure_ascii=False, indent=2))
