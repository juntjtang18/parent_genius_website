#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Extract Thymeleaf #{key} tokens from templates into messages.properties.

Captures keys from th:text, th:placeholder, th:title, th:attr, and
/*[[#{key}]]*/ inline JS. Merges into the existing English file: existing
values are never overwritten (keeps the translation cache valid). New keys
are appended; empty existing values are filled when a default is found.
"""

import html
import os
import re

KEY_RE = re.compile(r'#\{([A-Za-z][A-Za-z0-9_.-]*)\}')
INLINE_JS_RE = re.compile(
    r'/\*\[\[#\{([A-Za-z][A-Za-z0-9_.-]*)\}\]\]\*/\s*([\'"])(.*?)\2',
    re.DOTALL,
)
TH_TEXT_RE = re.compile(
    r'th:(?:text|utext)="#\{([A-Za-z][A-Za-z0-9_.-]*)\}"[^>]*>(.*?)</',
    re.DOTALL,
)
# Non-Thymeleaf fallbacks on the same opening tag as #{key}.
TAG_DEFAULT_RE = re.compile(
    r'(?<!th:)(?:placeholder|title|alt|aria-label|value|data-i18n-default)='
    r'(["\'])(.*?)\1',
    re.DOTALL,
)


def normalize_default(text):
    if not text:
        return ''
    return ' '.join(html.unescape(text).strip().split())


def set_default(found, key, value):
    value = normalize_default(value)
    if key not in found:
        found[key] = value
    elif not found[key] and value:
        found[key] = value


def enclosing_tag(content, index):
    start = content.rfind('<', 0, index)
    if start < 0:
        return ''
    end = content.find('>', start)
    if end < 0:
        return ''
    return content[start:end + 1]


def extract_keys(html_content):
    found = {}

    for key, _quote, default in INLINE_JS_RE.findall(html_content):
        set_default(found, key, default)

    for key, default in TH_TEXT_RE.findall(html_content):
        set_default(found, key, default)

    for match in KEY_RE.finditer(html_content):
        key = match.group(1)
        tag = enclosing_tag(html_content, match.start())
        for _quote, default in TAG_DEFAULT_RE.findall(tag):
            set_default(found, key, default)
        set_default(found, key, '')

    return found


def parse_properties(path):
    existing = {}
    if not os.path.isfile(path):
        return existing
    with open(path, 'r', encoding='utf-8') as handle:
        for line in handle:
            if not line.strip() or line.lstrip().startswith('#') or '=' not in line:
                continue
            key, value = line.rstrip('\n').split('=', 1)
            if key not in existing:
                existing[key] = value
    return existing


def fill_empty_values(path, extracted):
    """Replace key= (empty) with a newly found default. Leaves non-empty values alone."""
    if not os.path.isfile(path):
        return 0
    with open(path, 'r', encoding='utf-8') as handle:
        lines = handle.readlines()

    filled = 0
    new_lines = []
    for line in lines:
        stripped = line.rstrip('\n')
        if stripped and not stripped.lstrip().startswith('#') and '=' in stripped:
            key, value = stripped.split('=', 1)
            default = extracted.get(key, '')
            if value == '' and default:
                new_lines.append(f'{key}={default}\n')
                filled += 1
                continue
        new_lines.append(line)

    if filled:
        with open(path, 'w', encoding='utf-8') as handle:
            handle.writelines(new_lines)
    return filled


def append_new_keys(path, extracted, existing):
    new_items = [(key, extracted[key]) for key in extracted if key not in existing]
    if not new_items:
        return new_items
    new_items.sort(key=lambda item: item[0])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    prefix = ''
    if os.path.isfile(path) and os.path.getsize(path) > 0:
        with open(path, 'rb') as handle:
            handle.seek(-1, os.SEEK_END)
            if handle.read(1) != b'\n':
                prefix = '\n'
    with open(path, 'a', encoding='utf-8') as handle:
        handle.write(prefix)
        for key, value in new_items:
            handle.write(f'{key}={value}\n')
    return new_items


def process_directory(templates_dir):
    results = []
    combined = {}
    if not os.path.isdir(templates_dir):
        print(f"Error: Directory not found at '{templates_dir}'")
        print('Please make sure you are running the script from the correct location.')
        return results, combined

    for root, _, files in os.walk(templates_dir):
        for name in sorted(files):
            if not name.endswith('.html'):
                continue
            file_path = os.path.join(root, name)
            with open(file_path, 'r', encoding='utf-8') as handle:
                extracted = extract_keys(handle.read())
            for key, value in extracted.items():
                set_default(combined, key, value)
            results.append((file_path, len(extracted)))
    return results, combined


def main():
    base_dir = os.getcwd()
    templates_directory = os.path.join(base_dir, 'templates')
    properties_file = os.path.join(base_dir, 'messages', 'messages.properties')

    results, extracted = process_directory(templates_directory)
    existing = parse_properties(properties_file)
    filled = fill_empty_values(properties_file, extracted)
    existing = parse_properties(properties_file)
    added = append_new_keys(properties_file, extracted, existing)

    for file_path, count in results:
        print(f'{file_path}: {count}')

    missing_default = sorted(
        key for key, value in extracted.items()
        if not (value or existing.get(key))
    )
    print(f'\nkeys found: {len(extracted)}')
    print(f'existing keys kept: {len(existing)}')
    print(f'empty values filled: {filled}')
    print(f'new keys appended: {len(added)}')
    if added:
        for key, value in added:
            print(f'  + {key}={value}')
    if missing_default:
        print('keys with no default (add /*[[#{key}]]*/ \'…\' or inner text):')
        for key in missing_default:
            print(f'  ? {key}')


if __name__ == '__main__':
    main()
