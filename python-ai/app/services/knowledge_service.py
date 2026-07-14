import json
from pathlib import Path

# Project root (study-assistant/)
PROJECT_ROOT = Path(__file__).resolve().parents[3]

PROCESSED_DIR = PROJECT_ROOT / "backend" / "processed"


def load_knowledge(upload_id):

    knowledge_path = PROCESSED_DIR / upload_id / "knowledge.json"

    print("Loading knowledge from:", knowledge_path)

    if not knowledge_path.exists():
        raise FileNotFoundError(f"knowledge.json not found: {knowledge_path}")

    with open(knowledge_path, "r", encoding="utf-8") as file:
        return json.load(file)