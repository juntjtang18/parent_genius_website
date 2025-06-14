import os
import re
import codecs
import concurrent.futures
from deep_translator import GoogleTranslator

# --- USER CONFIGURATION ---
# Adjust the number of concurrent threads. A good starting point is 10.
# Increase this if you have a fast internet connection.
MAX_WORKERS = 10 
# --------------------------

# List of target languages and their codes
TARGET_LANGUAGES = {
    'fr': 'French',
    'es': 'Spanish',
    'zh-CN': 'Chinese (Simplified)',
    'de': 'German',
    'ko': 'Korean',
    'ja': 'Japanese',
    'si': 'Sinhala',  # Official language of Sri Lanka
    'hi': 'Hindi',    # Widely spoken in India
    'bn': 'Bengali',  # Also widely spoken in India
}

# Regular expression to match files with language suffixes (e.g., *_fr.properties)
LANG_SUFFIX_PATTERN = re.compile(r'.*_[a-z]{2}(-[A-Z]{2})?\.properties$')

def to_unicode_escape(text):
    """Convert text to Unicode escape sequences."""
    return text.encode('unicode_escape').decode('ascii')

def translate_text(text, lang_code):
    """Translate text using Google Translate."""
    if not text:
        return ""
    try:
        # Note: Initializing the translator for every call is required for thread safety.
        return GoogleTranslator(source='auto', target=lang_code).translate(text)
    except Exception as e:
        print(f"⚠️ Error translating text: {text} ({e})")
        return text  # Fallback to original text on error

def translate_properties_file(input_file, lang_code):
    """
    Translate a single .properties file to the target language using a thread pool
    for concurrent network requests.
    """
    output_file = f"{os.path.splitext(input_file)[0]}_{lang_code.replace('-', '_')}.properties"
    is_chinese = lang_code == 'zh-CN'

    lines_to_process = []
    other_lines = [] # For comments, blank lines, etc.
    
    # --- MODIFIED: First, read all lines to prepare for batch processing ---
    with codecs.open(input_file, 'r', 'utf-8') as infile:
        for i, line in enumerate(infile):
            if '=' in line and not line.strip().startswith('#'):
                key, value = line.strip().split('=', 1)
                lines_to_process.append({'key': key, 'value': value, 'index': i})
            else:
                other_lines.append({'line': line, 'index': i})

    values_to_translate = [item['value'] for item in lines_to_process]
    translated_values = []

    # --- NEW: Use ThreadPoolExecutor to translate all values concurrently ---
    if values_to_translate:
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            # Create a future for each translation task
            future_to_value = {executor.submit(translate_text, value, lang_code): value for value in values_to_translate}
            
            # Use a dictionary to map original values to translated ones
            # This is more robust than relying on the order of results from executor.map
            results_map = {}
            for future in concurrent.futures.as_completed(future_to_value):
                original_value = future_to_value[future]
                try:
                    translated_value = future.result()
                    results_map[original_value] = translated_value
                except Exception as exc:
                    print(f"⚠️ Generated an exception: {original_value} -> {exc}")
                    results_map[original_value] = original_value # Fallback

            # Reconstruct translated values in the original order
            translated_values = [results_map.get(val, val) for val in values_to_translate]

    # Combine translated lines with other lines and sort them to maintain original file order
    final_lines = []
    for i, item in enumerate(lines_to_process):
        translated_text = translated_values[i]
        if is_chinese and translated_text:
            translated_text = to_unicode_escape(translated_text)
        final_lines.append({'line': f"{item['key']}={translated_text}\n", 'index': item['index']})
    
    final_lines.extend(other_lines)
    final_lines.sort(key=lambda x: x['index'])

    # --- MODIFIED: Write all processed lines at once ---
    with codecs.open(output_file, 'w', 'utf-8') as outfile:
        for item in final_lines:
            outfile.write(item['line'])

    print(f"✅ Translated file saved: {output_file}")


def process_directory(input_dir):
    """Scan directory recursively and translate all eligible .properties files."""
    for root, dirs, files in os.walk(input_dir):
        for file in files:
            if file == 'application.properties':
                print(f"❌ Skipping 'application.properties'.")
                continue

            # Only process base English .properties files without language suffixes
            if file.endswith('.properties') and not LANG_SUFFIX_PATTERN.match(file):
                file_path = os.path.join(root, file)
                print(f"\n🔍 Processing: {file_path}")

                for lang_code, lang_name in TARGET_LANGUAGES.items():
                    print(f" - Translating to {lang_name} ({lang_code})")
                    # --- MODIFIED: The function call is the same, but the underlying implementation is now concurrent ---
                    translate_properties_file(file_path, lang_code)

if __name__ == '__main__':
    default_directory = os.getcwd()
    input_directory = input(f"📁 Enter the path to your properties folder (or press Enter to use current directory: '{default_directory}'): ").strip()

    if not input_directory:
        input_directory = default_directory

    if os.path.isdir(input_directory):
        process_directory(input_directory)
        print("\n✅ Translation completed for all files and languages.")
    else:
        print(f"❌ Invalid directory: {input_directory}. Please check the path.")