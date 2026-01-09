import os
import re
import time
import json
import codecs
import random
import threading
import concurrent.futures
from deep_translator import GoogleTranslator

# ------------------ CONFIG ------------------
LINE_WORKERS = 20               # increase to test speed; try 20–40
RETRIES = 3
BACKOFF_MIN = 0.5               # seconds
BACKOFF_MAX = 1.5               # seconds

# Output suffix overrides (translator still uses original lang code)
OUTPUT_SUFFIX_OVERRIDES = {
    'zh-CN': 'zh',
}

TARGET_LANGUAGES = {
    'fr': 'French',
    'es': 'Spanish',
    'zh-CN': 'Chinese (Simplified)',
    'de': 'German',
    'ko': 'Korean',
    'ja': 'Japanese',
    'si': 'Sinhala',
    'hi': 'Hindi',
    'bn': 'Bengali',
}

LANG_SUFFIX_PATTERN = re.compile(r'.*_[a-z]{2}(-[A-Z]{2})?\.properties$')
CACHE_DIR = os.path.join('.cache')
os.makedirs(CACHE_DIR, exist_ok=True)

_cache_lock = threading.Lock()  # guard cache writes

# ------------------ HELPERS ------------------

def to_unicode_escape(text: str) -> str:
    return text.encode('unicode_escape').decode('ascii')

def _cache_path(lang_code: str) -> str:
    # file per language; map original English value -> translated text
    safe = lang_code.replace('/', '_')
    return os.path.join(CACHE_DIR, f"translations_{safe}.json")

def load_cache(lang_code: str) -> dict:
    path = _cache_path(lang_code)
    if os.path.isfile(path):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception:
            return {}
    return {}

def save_cache(lang_code: str, cache: dict):
    path = _cache_path(lang_code)
    with _cache_lock:
        tmp = path + '.tmp'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(cache, f, ensure_ascii=False, indent=0)
        os.replace(tmp, path)

def translate_with_retry(text: str, lang_code: str) -> str:
    if not text:
        return ""
    for attempt in range(1, RETRIES + 1):
        try:
            # Instantiate per call for thread-safety
            return GoogleTranslator(source='auto', target=lang_code).translate(text)
        except Exception as e:
            if attempt == RETRIES:
                print(f"⚠️  Giving up after {RETRIES} tries on: {text[:60]}... ({e})")
                return text
            time.sleep(random.uniform(BACKOFF_MIN, BACKOFF_MAX) * attempt)

def translate_properties_file(input_file: str, lang_code: str):
    # Decide output suffix
    default_suffix = lang_code.replace('-', '_')
    output_suffix = OUTPUT_SUFFIX_OVERRIDES.get(lang_code, default_suffix)
    output_file = f"{os.path.splitext(input_file)[0]}_{output_suffix}.properties"
    is_chinese = lang_code.lower().startswith('zh')

    # Read file
    lines_to_process = []
    passthrough = []
    with codecs.open(input_file, 'r', 'utf-8') as infile:
        for i, line in enumerate(infile):
            if '=' in line and not line.strip().startswith('#'):
                key, value = line.rstrip('\n').split('=', 1)
                lines_to_process.append({'key': key, 'value': value, 'index': i})
            else:
                passthrough.append({'line': line, 'index': i})

    # Load per-language cache (English value -> translated)
    cache = load_cache(lang_code)

    # Figure out what needs translation (skip cached identical English strings)
    to_translate_indices = []
    to_translate_values = []
    translated_values = [None] * len(lines_to_process)

    for idx, item in enumerate(lines_to_process):
        v = item['value']
        if v in cache and cache[v] is not None:
            translated_values[idx] = cache[v]
        else:
            to_translate_indices.append(idx)
            to_translate_values.append(v)

    # Parallel translate the missing ones
    if to_translate_values:
        with concurrent.futures.ThreadPoolExecutor(max_workers=LINE_WORKERS) as pool:
            future_map = {pool.submit(translate_with_retry, v, lang_code): (i, v)
                          for i, v in zip(to_translate_indices, to_translate_values)}
            for fut in concurrent.futures.as_completed(future_map):
                idx, original = future_map[fut]
                try:
                    translated = fut.result()
                except Exception as exc:
                    print(f"⚠️  Unexpected error translating line: {original[:60]}... -> {exc}")
                    translated = original
                translated_values[idx] = translated
                cache[original] = translated  # update cache per English value

    # Persist updated cache
    save_cache(lang_code, cache)

    # Rebuild file preserving order
    final_lines = []
    for i, item in enumerate(lines_to_process):
        text = translated_values[i]
        if is_chinese and text:
            text = to_unicode_escape(text)
        final_lines.append({'line': f"{item['key']}={text}\n", 'index': item['index']})

    final_lines.extend(passthrough)
    final_lines.sort(key=lambda x: x['index'])

    with codecs.open(output_file, 'w', 'utf-8') as out:
        for entry in final_lines:
            out.write(entry['line'])

    print(f"✅ Saved: {output_file}")

def process_directory(input_dir: str):
    """One file at a time; inside each file, per-line parallelism."""
    for root, _, files in os.walk(input_dir):
        for file in files:
            if file == 'application.properties':
                print("❌ Skipping 'application.properties'.")
                continue
            if file.endswith('.properties') and not LANG_SUFFIX_PATTERN.match(file):
                base_path = os.path.join(root, file)
                print(f"\n🔍 Processing base file: {base_path}")
                for lang_code, lang_name in TARGET_LANGUAGES.items():
                    print(f"  → {lang_name} ({lang_code})")
                    translate_properties_file(base_path, lang_code)

if __name__ == '__main__':
    input_directory = os.getcwd()
    if os.path.isdir(input_directory):
        process_directory(input_directory)
        print("\n✅ All translations complete.")
    else:
        print(f"❌ Invalid directory: {input_directory}.")
