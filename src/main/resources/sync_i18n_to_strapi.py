#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os, re, sys, json, codecs, shutil, requests, subprocess
from typing import Dict, Tuple

# ------------ CONFIG ------------
STRAPI_BASE = os.environ.get("STRAPI_BASE", "http://localhost:8080").rstrip("/")
STRAPI_TOKEN = os.environ.get("STRAPI_TOKEN")  # required
STRAPI_TOKEN = "0e13989e4e21c5d4e4047933446308f2646e224b2612e26d17fe3783fe230005e5e90e22d7c803a1fa452309bd81bd4f58d3b47a3bd74239d0127c5dcd803e092092f2442cf38134f8d324cb237a69917cccd341e9182b550c19847fc1079f524aaa53684990eba80a78079bb96e5f06c1d8b196fd226e91066cbe2fbe53d7c4"
COLL = "webpage-captions"
PAGE_SIZE = int(os.environ.get("STRAPI_PAGE_SIZE", "200"))
REQ_TIMEOUT = int(os.environ.get("STRAPI_REQ_TIMEOUT", "60"))

BASE_DIR = os.getcwd()
MESSAGES_DIR = os.path.join(BASE_DIR, "messages")
TMP_DIR = os.path.join(BASE_DIR, "tmp")   # <— use tmp for transient translations
EN_FILE = os.path.join(MESSAGES_DIR, "messages.properties")
I18N_SCRIPT = os.path.join(BASE_DIR, "i18n.py")
TRANSLATE_SCRIPT = os.path.join(BASE_DIR, "translate_openai_properties.py")

TEST_MODE = True

U_ESC = re.compile(r"\\u([0-9a-fA-F]{4})")

def unescape_java_unicode(s: str) -> str:
    if not s: return s
    return U_ESC.sub(lambda m: chr(int(m.group(1), 16)), s)

def to_unicode_escape(s: str) -> str:
    return "".join(f"\\u{ord(c):04x}" if ord(c) > 127 else c for c in s)

def die(msg, code=2):
    print(f"ERROR: {msg}", file=sys.stderr); sys.exit(code)

def run(cmd, cwd=None):
    p = subprocess.run(cmd, cwd=cwd)
    if p.returncode != 0: die(f"Command failed: {' '.join(cmd)} (rc={p.returncode})")

def parse_properties(path: str) -> Dict[str, str]:
    out = {}
    with codecs.open(path, "r", "utf-8") as f:
        for line in f:
            if not line.strip() or line.lstrip().startswith("#"): continue
            if "=" not in line: continue
            k, v = line.rstrip("\n").split("=", 1)
            out[k] = v
    return out

def write_properties(path: str, kv: Dict[str, str], escape_zh=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with codecs.open(path, "w", "utf-8") as f:
        for k in sorted(kv.keys()):
            v = kv[k]
            if escape_zh: v = to_unicode_escape(v)
            f.write(f"{k}={v}\n")

def rest_get(url, params=None):
    headers = {"Authorization": f"Bearer {STRAPI_TOKEN}"} if STRAPI_TOKEN else {}
    r = requests.get(url, headers=headers, params=params or {}, timeout=REQ_TIMEOUT)
    r.raise_for_status(); return r.json()

def rest_post(url, data: dict):
    headers = {"Authorization": f"Bearer {STRAPI_TOKEN}", "Content-Type": "application/json", "Accept": "application/json"}
    r = requests.post(url, headers=headers, data=json.dumps(data), timeout=REQ_TIMEOUT)
    r.raise_for_status(); return r.json()

def rest_post_flat(url, payload: dict, extra_headers: dict = None):
    headers = {"Authorization": f"Bearer {STRAPI_TOKEN}", "Content-Type": "application/json", "Accept": "application/json"}
    if extra_headers: headers.update(extra_headers)
    r = requests.post(url, headers=headers, data=json.dumps(payload), timeout=REQ_TIMEOUT)
    r.raise_for_status(); return r.json()

def rest_put(url, data: dict):
    headers = {"Authorization": f"Bearer {STRAPI_TOKEN}", "Content-Type": "application/json", "Accept": "application/json"}
    r = requests.put(url, headers=headers, data=json.dumps(data), timeout=REQ_TIMEOUT)
    r.raise_for_status(); return r.json()

def ensure_en_file():
    os.makedirs(MESSAGES_DIR, exist_ok=True)
    if TEST_MODE:
        src = os.path.join(BASE_DIR, "messages.properties")
        print(f"[sync] TEST_MODE=1 — copying messages.properties into ./messages/")
        if not os.path.isfile(src): die(f"{src} not found")
        shutil.copyfile(src, EN_FILE)
    else:
        if not os.path.isfile(I18N_SCRIPT): die(f"i18n.py not found at {I18N_SCRIPT}")
        print("[sync] running i18n.py …")
        run([sys.executable, I18N_SCRIPT], cwd=BASE_DIR)
    if not os.path.isfile(EN_FILE): die(f"EN properties file missing at {EN_FILE}")

def discover_locales_from_tmp(tmp_dir: str):
    locales = []
    for name in os.listdir(tmp_dir):
        m = re.match(r"^messages_([A-Za-z]{2}(?:_[A-Za-z]{2})?)\.properties$", name)
        if not m: continue
        suffix = m.group(1)
        loc = suffix.replace("_", "-")
        if loc.lower().startswith("zh"): loc = "zh"
        if loc != "en": locales.append(loc)
    seen, out = set(), []
    for l in locales:
        if l not in seen:
            out.append(l); seen.add(l)
    return out

def load_existing_for_locale(locale: str) -> Dict[str, Tuple[int, str]]:
    url = f"{STRAPI_BASE}/api/{COLL}"
    page = 1; res_map = {}
    while True:
        params = {"locale": locale, "pagination[page]": page, "pagination[pageSize]": PAGE_SIZE,
                  "fields[0]": "key", "fields[1]": "caption"}
        obj = rest_get(url, params=params)
        data = obj.get("data", []); pg = obj.get("meta", {}).get("pagination", {})
        for item in data:
            iid = item.get("id"); attrs = item.get("attributes", {})
            k = attrs.get("key"); cap = attrs.get("caption") or ""
            if iid and k: res_map[k] = (iid, cap)
        if not pg or pg.get("page", page) >= pg.get("pageCount", page): break
        page += 1
    return res_map

def fetch_strapi_locales() -> list[str]:
    """Fetches all enabled locale codes from Strapi's i18n plugin."""
    url = f"{STRAPI_BASE}/api/i18n/locales"
    print(f"[sync] fetching locales from {url} ...")
    try:
        data = rest_get(url)
        # data is typically a list of locale objects, e.g., [{"id": 1, "code": "en", ...}]
        if isinstance(data, list) and all(isinstance(item, dict) and 'code' in item for item in data):
            codes = [item['code'] for item in data]
        else:
            print("Warning: Unexpected response from /api/i18n/locales. Assuming 'en'.")
            return ['en']
        
        if 'en' not in codes:
            codes.insert(0, 'en') # Ensure 'en' is present
        return list(dict.fromkeys(codes)) # Return unique list, preserving order
    except requests.HTTPError as e:
        if e.response.status_code == 404:
            print("Warning: /api/i18n/locales not found. Is i18n plugin enabled? Defaulting to 'en'.")
            return ['en']
        else:
            raise e

def main():
    if not STRAPI_TOKEN: die("STRAPI_TOKEN env var is required")

    # 1) Ensure EN file
    ensure_en_file()

    # 2) Translate to TMP (transient)
    os.makedirs(TMP_DIR, exist_ok=True)
    print(f"[sync] running translate_openai_properties.py at: {TRANSLATE_SCRIPT} …")
    run([sys.executable, TRANSLATE_SCRIPT, "--out-dir", TMP_DIR], cwd=BASE_DIR)

    # 3) Read EN + TMP locales
    print("[sync] reading properties …")
    en_map = parse_properties(EN_FILE)

    locales = discover_locales_from_tmp(TMP_DIR)
    loc_maps = {}
    for loc in locales:
        suffix = "zh" if loc == "zh" else loc.replace("-", "_")
        path = os.path.join(TMP_DIR, f"messages_{suffix}.properties")
        m = parse_properties(path) if os.path.isfile(path) else {}
        if loc == "zh":  # store visible Chinese in Strapi
            m = {k: unescape_java_unicode(v) for k, v in m.items()}
        loc_maps[loc] = m
    print(f"[sync] locales discovered: {locales or '[]'}")

    # 4) Load Strapi existing maps
    print("[sync] loading existing Strapi records (EN) …")
    existing_en = load_existing_for_locale("en")
    existing_by_locale = {"en": existing_en}
    for loc in locales:
        print(f"[sync] loading existing Strapi records ({loc}) …")
        existing_by_locale[loc] = load_existing_for_locale(loc)

    # 5) Upsert EN
    # FIX: This entire block has been de-dented to be outside the loop above
    print("[sync] upserting EN …")
    url_base = f"{STRAPI_BASE}/api/{COLL}"
    en_id_by_key = {}
    created_en = updated_en = 0

    for key, en_caption in en_map.items():
        entry = existing_en.get(key)
        if entry is None:
            payload = {"data": {"key": key, "caption": en_caption, "locale": "en", "changed": False}}
            resp = rest_post(url_base, payload)
            en_id = resp.get("data", {}).get("id")
            if not en_id: die(f"Failed to create EN for key={key}")
            en_id_by_key[key] = en_id
            created_en += 1
        else:
            en_id, old_caption = entry
            if (old_caption or "") != (en_caption or ""):
                # mark changed=true on EN when its caption changed
                rest_put(f"{url_base}/{en_id}", {"data": {"caption": en_caption, "changed": True}})
                updated_en += 1
            # This line (which you already had) is correct
            en_id_by_key[key] = en_id

    # 6) Upsert missing locales ONLY from TMP
    print("[sync] upserting locales …")
    created_loc = {loc: 0 for loc in locales}
    skipped_loc = {loc: 0 for loc in locales}
    for loc in locales:
        existing_loc = existing_by_locale.get(loc, {})
        loc_map = loc_maps.get(loc, {})
        for key in en_map.keys():
            if key in existing_loc:
                skipped_loc[loc] += 1; continue
            val = loc_map.get(key)
            if val is None: continue
            en_id = en_id_by_key.get(key)
            if not en_id: continue
            # Strapi i18n: flat body + Content-Language header
            loc_url = f"{STRAPI_BASE}/api/{COLL}/{en_id}/localizations"
            payload = {"caption": val, "locale": loc, "changed": False}
            rest_post_flat(loc_url, payload, {"Content-Language": loc})
            created_loc[loc] += 1

    # 7) Fetch all enabled locale codes from Strapi
    print("[sync] fetching all enabled locales from Strapi …")
    strapi_locales = fetch_strapi_locales()
    print(f"[sync] found locales in Strapi: {strapi_locales}")

    # 8) Fetch each locale from Strapi and write canonical ./messages/*.properties
    print("[sync] fetching all records per locale and writing files …")
    
    for loc in strapi_locales:
        print(f"  ... fetching '{loc}'")
        # This function already handles pagination and returns the full map {key: (id, caption)}
        kv_map = load_existing_for_locale(loc)
        
        if not kv_map:
            print(f"  ... no records found for '{loc}', skipping file write.")
            continue

        # Convert {key: (id, caption)} to {key: caption} for writing
        final_kv_map = {k: v[1] for k, v in kv_map.items()}

        if loc == "en":
            out_path = os.path.join(MESSAGES_DIR, "messages.properties")
            escape = False
        else:
            suffix = "zh" if loc == "zh" else loc.replace("-", "_")
            out_path = os.path.join(MESSAGES_DIR, f"messages_{suffix}.properties")
            escape = (loc == "zh")
            
        write_properties(out_path, final_kv_map, escape_zh=escape)
        print(f"  ... wrote {len(final_kv_map)} keys to {out_path}")


    # Summary
    print("\n=== sync summary ===")
    print(f"EN created: {created_en}")
    print(f"EN updated: {updated_en}")
    for loc in locales:
        print(f"{loc} created: {created_loc[loc]} (skipped existing: {skipped_loc[loc]})")
    print("====================")

if __name__ == "__main__":
    try:
        main()
    except requests.HTTPError as e:
        body = getattr(e.response, "text", str(e))
        die(f"HTTP : {body}", 3)