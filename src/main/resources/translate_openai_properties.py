
#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os, sys, re, json, time, codecs, argparse, concurrent.futures
from typing import Dict, List, Tuple

# --- CONFIGURATION ---

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MESSAGES_DIR = os.path.join(BASE_DIR, "messages")
EN_FILE = os.path.join(MESSAGES_DIR, "messages.properties")

# Path to your human-readable, manually-edited Chinese file
ZH_EDIT_FILE = os.path.join(BASE_DIR, "persistent", "messages_zh-edit.properties")

# Use 'zh' as requested, not 'zh-CN'
TARGETS = {
    "fr": "fr",
    "es": "es",
    "de": "de",
    "ko": "ko",
    "ja": "ja",
    "si": "si",
    "hi": "hi",
    "bn": "bn",
    "zh": "zh", # Use "zh" for OpenAI target
}

OPENAI_MODEL = os.environ.get("OPENAI_TRANSLATE_MODEL", "gpt-4o-mini")
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")


BATCH_SIZE = int(os.environ.get("OPENAI_BATCH_SIZE", "48"))
MAX_PAR_LOCALES = int(os.environ.get("OPENAI_MAX_PAR_LOCALES", "4"))

# --- HELPER FUNCTIONS ---

def parse_properties(path: str) -> Dict[str, str]:
    """Reads a .properties file into a dictionary."""
    out = {}
    if not os.path.isfile(path):
        return out
    with codecs.open(path, "r", "utf-8") as f:
        for line in f:
            if not line.strip() or line.lstrip().startswith("#"): continue
            if "=" not in line: continue
            k, v = line.rstrip("\n").split("=", 1)
            out[k.strip()] = v.strip()
    return out

def to_unicode_escape(s: str) -> str:
    """Escapes strings for Spring Boot .properties files."""
    return "".join(f"\\u{ord(c):04x}" if ord(c) > 127 else c for c in s)

def write_properties(path: str, kv: Dict[str, str], escape_output: bool = True):
    """
    Writes a dictionary to a .properties file.
    If escape_output=False, writes in human-readable UTF-8.
    If escape_output=True, writes in escaped unicode.
    """
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with codecs.open(path, "w", "utf-8") as f:
        for k in sorted(kv.keys()):
            v = kv.get(k, "")
            if escape_output:
                v = to_unicode_escape(v)
            f.write(f"{k}={v}\n")

def load_openai_client():
    from openai import OpenAI
    api_key = OPENAI_API_KEY or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        print("❌ OPENAI_API_KEY missing."); sys.exit(4)
    return OpenAI(api_key=api_key)

# --- OPENAI TRANSLATION ---

def mk_prompt(items: List[Tuple[str, str]], target_lang: str) -> str:
    pairs = [{"key": k, "en": v} for k, v in items]
    return (
        "You are a professional localization engine. Translate each English string to the target language. "
        "Preserve placeholders like {0}, {1}, HTML tags, and line breaks. "
        "Return ONLY a valid JSON object of key->translated string.\n"
        f"Target language code: {target_lang}\nData:\n{json.dumps(pairs, ensure_ascii=False)}"
    )

def translate_batch_openai(client, items: List[Tuple[str, str]], target_lang: str) -> Dict[str, str]:
    if not items:
        return {}
    prompt = mk_prompt(items, target_lang)
    try:
        resp = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": "You are a translation engine. Return only JSON."},
                {"role": "user", "content": prompt},
            ],
            temperature=0.2,
            response_format={"type": "json_object"},
        )
        content = resp.choices[0].message.content.strip()
        data = json.loads(content)
        # Ensure all requested keys are present in the response
        return {k: data.get(k, en_text) for k, en_text in items}
    except Exception as e:
        print(f"    ❌ ERROR during OpenAI translation: {e}")
        print(f"    Content received: {content[:100]}...")
        # Fallback: return English text for failed keys
        return {k: en_text for k, en_text in items}

def run_translation(client, base_map: Dict[str, str], keys_to_translate: List[str], target_lang: str) -> Dict[str, str]:
    """Helper to run translation tasks in batches."""
    items = [(k, base_map[k]) for k in keys_to_translate]
    results = {}
    total_batches = (len(items) + BATCH_SIZE - 1) // BATCH_SIZE
    
    for i in range(0, len(items), BATCH_SIZE):
        batch_num = (i // BATCH_SIZE) + 1
        print(f"    - Batch {batch_num}/{total_batches} ({len(items[i:i+BATCH_SIZE])} items)...", end="", flush=True)
        t0 = time.time()
        chunk = items[i:i+BATCH_SIZE]
        translated = translate_batch_openai(client, chunk, target_lang)
        dt = time.time() - t0
        print(f" done in {dt:.1f}s")
        results.update(translated)
        
    return results

# --- LOCALE HANDLERS ---

def handle_standard_locale(client, out_dir: str, base_map: Dict[str, str], suffix: str, target: str, force: bool):
    """
    Translates a standard locale (non-zh).
    Merges with existing translations and saves in escaped format.
    """
    print(f"\nProcessing standard locale: {suffix} -> {target}")
    out_path = os.path.join(out_dir, f"messages_{suffix}.properties")
    
    existing_map = parse_properties(out_path)
    
    if force:
        keys_to_translate = list(base_map.keys())
        print("    - Force enabled, translating all keys.")
    else:
        keys_to_translate = [k for k in base_map.keys() if k not in existing_map or not existing_map[k]]
        print(f"    - Found {len(existing_map)} existing keys.")
        print(f"    - Translating {len(keys_to_translate)} new or missing keys.")

    if not keys_to_translate:
        print("    - No new keys to translate.")
        return

    newly_translated_map = run_translation(client, base_map, keys_to_translate, target)
    
    # Merge old and new, new translations overwrite
    final_map = {**existing_map, **newly_translated_map}
    
    # Write the final escaped file for Spring Boot
    write_properties(out_path, final_map, escape_output=True)
    print(f"✅ Saved standard locale: {out_path}")

def handle_zh_translation(client, out_dir: str, base_map: Dict[str, str], target: str, force: bool):
    """
    Translates Chinese (zh), preserving manual edits from messages_zh-edit.properties.
    Saves both a UTF-8 edit file and an escaped Spring Boot file.
    """
    print(f"\nProcessing special locale: zh -> {target}")
    
    # This is your final, complete map for 'zh'
    final_zh_map = {}
    
    # 1. Load your existing manual edits
    existing_manual_edits = parse_properties(ZH_EDIT_FILE)
    print(f"    - Found {len(existing_manual_edits)} manually edited keys in {ZH_EDIT_FILE}")

    # 2. Find out which keys need to be translated
    keys_to_translate = []
    if force:
        print("    - Force enabled, will re-translate all keys.")
        keys_to_translate = list(base_map.keys())
    else:
        for key in base_map.keys():
            # If key is not in the edit file, or it is but the value is empty
            if key not in existing_manual_edits or not existing_manual_edits[key]:
                keys_to_translate.append(key)
            else:
                # Preserve the existing manual edit
                final_zh_map[key] = existing_manual_edits[key]
        print(f"    - Preserving {len(final_zh_map)} existing manual edits.")
        print(f"    - Translating {len(keys_to_translate)} new or missing keys.")

    # 3. Translate only the missing keys
    if keys_to_translate:
        newly_translated_map = run_translation(client, base_map, keys_to_translate, target)
        # Add the new translations to the final map
        final_zh_map.update(newly_translated_map)
    else:
        print("    - No new keys to translate.")

    # 4. Write the human-readable file for you to edit (UTF-8)
    write_properties(ZH_EDIT_FILE, final_zh_map, escape_output=False)
    print(f"✅ Saved human-readable edit file: {ZH_EDIT_FILE}")
    
    # 5. Write the escaped file for Spring Boot
    spring_boot_zh_path = os.path.join(out_dir, "messages_zh.properties")
    write_properties(spring_boot_zh_path, final_zh_map, escape_output=True)
    print(f"✅ Saved Spring Boot file: {spring_boot_zh_path}")


# --- MAIN EXECUTION ---

def main():
    ap = argparse.ArgumentParser()
    # Default output is now the 'messages' directory
    ap.add_argument("--out-dir", default=MESSAGES_DIR, help="directory to write translated .properties")
    ap.add_argument("--force", action="store_true", help="Force re-translation of all keys, ignoring cache.")
    args = ap.parse_args()

    if not os.path.isfile(EN_FILE):
        print(f"❌ Base file missing: {EN_FILE}"); sys.exit(2)

    base_map = parse_properties(EN_FILE)
    if not base_map:
        print("❌ No base keys found."); sys.exit(3)

    print(f"\n🔍 Translating base file: {EN_FILE} ({len(base_map)} keys)")
    client = load_openai_client()

    targets = list(TARGETS.items())
    print(f"  … will translate {len(targets)} locale(s) in parallel (max {MAX_PAR_LOCALES})")

    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_PAR_LOCALES) as pool:
        futs = []
        for suffix, target in targets:
            if suffix == "zh":
                # Run 'zh' translation in the main thread to avoid conflicts
                # (or submit to pool if you are confident, but this is safer)
                continue
            
            print(f"  → Queuing: {target} ({suffix})")
            futs.append(pool.submit(
                handle_standard_locale, 
                client, args.out_dir, base_map, suffix, target, args.force
            ))
        
        # Wait for all standard locales to finish
        for fut in concurrent.futures.as_completed(futs):
            try:
                fut.result() # Check for exceptions
            except Exception as e:
                print(f"    ❌ ERROR in thread: {e}")
        
        # --- Run 'zh' last and separately ---
        # This prevents race conditions and is clearer
        handle_zh_translation(client, args.out_dir, base_map, TARGETS['zh'], args.force)


    print("\n✅ All translations complete.")

if __name__ == "__main__":
    main()
