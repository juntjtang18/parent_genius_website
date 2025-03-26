import os
import re
import codecs
from deep_translator import GoogleTranslator

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
    try:
        translated = GoogleTranslator(source='auto', target=lang_code).translate(text)
        return translated
    except Exception as e:
        print(f"⚠️ Error translating text: {text} ({e})")
        return text  # Fallback to original text on error

def translate_properties_file(input_file, lang_code):
    """Translate a single .properties file to the target language."""
    output_file = f"{os.path.splitext(input_file)[0]}_{lang_code.replace('-', '_')}.properties"  # Adjust file naming for zh_CN
    is_chinese = lang_code == 'zh-CN'  # Flag for Chinese-specific handling

    with codecs.open(input_file, 'r', 'utf-8') as infile, \
         codecs.open(output_file, 'w', 'utf-8') as outfile:

        for line in infile:
            if not line.strip() or line.strip().startswith('#'):
                outfile.write(line)
                continue

            if '=' in line:
                key, value = line.strip().split('=', 1)
                translated_text = translate_text(value, lang_code)
                if translated_text:  # Ensure translation succeeded
                    if is_chinese:
                        # For Chinese, convert to Unicode escape sequences
                        translated_text = to_unicode_escape(translated_text)
                    outfile.write(f"{key}={translated_text}\n")
                else:
                    # Fallback to original value if translation fails
                    outfile.write(f"{key}={value}\n")
            else:
                outfile.write(line)

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
                    translate_properties_file(file_path, lang_code)

if __name__ == '__main__':
    # --- USER CONFIGURATION ---
    default_directory = os.getcwd()  # Use current working directory as default
    input_directory = input(f"📁 Enter the path to your properties folder (or press Enter to use current directory: '{default_directory}'): ").strip()

    if not input_directory:
        input_directory = default_directory
    # --------------------------

    if os.path.isdir(input_directory):
        process_directory(input_directory)
        print("\n✅ Translation completed for all files and languages.")
    else:
        print(f"❌ Invalid directory: {input_directory}. Please check the path.")
