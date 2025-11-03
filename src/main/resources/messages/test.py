from deep_translator import GoogleTranslator

try:
    result = GoogleTranslator(source='en', target='fr').translate("Hello, this is a test.")
    print("✅ Translation works:", result)
except Exception as e:
    print("❌ Translation failed:", e)
