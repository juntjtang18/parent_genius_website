import os
import re
import codecs

# Function to process HTML files and extract messages
def process_html_file(file_path, properties_file, is_first_file):
    with codecs.open(file_path, 'r', 'utf-8') as infile:
        html_content = infile.read()

    # --- MODIFIED: Added a third option to your original regex for placeholders ---
    pattern = re.compile(
        # Your original pattern for th:text
        r'th:text="#\{([^}]+)\}"[^>]*>(.*?)</.*?>|'
        r'th:text="#\{([^}]+)\}"[^>]*>([^<]+)<|'
        # The new pattern for th:placeholder with data-i18n-default
        r'th:placeholder="#\{([^}]+)\}".*?data-i18n-default="([^"]+)"'
    )
    matches = pattern.findall(html_content)

    written_pairs = 0
    if matches:
        mode = 'w' if is_first_file else 'a'
        with codecs.open(properties_file, mode, 'utf-8') as prop_file:
            for match in matches:
                # This now checks the extra capture groups from the new pattern
                key = match[0] or match[2] or match[4]
                message = match[1] or match[3] or match[5]
                
                # The rest of your logic is unchanged
                message = ' '.join(message.strip().splitlines())
                
                prop_file.write(f"{key}={message}\n")
                written_pairs += 1

    return file_path, written_pairs

# This function is unchanged
def process_directory(directory, properties_file):
    is_first_file = True
    results = []
    
    if not os.path.isdir(directory):
        print(f"Error: Directory not found at '{directory}'")
        print("Please make sure you are running the script from the correct location.")
        return results

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.html'):
                file_path = os.path.join(root, file)
                result = process_html_file(file_path, properties_file, is_first_file)
                results.append(result)
                # This logic ensures the first file overwrites, subsequent files append
                if result[1] > 0 and is_first_file:
                    is_first_file = False

    return results

# This `main` block is exactly your original version
if __name__ == '__main__':
    base_dir = os.getcwd()
    templates_directory = os.path.join(base_dir, 'templates')
    properties_file = os.path.join(base_dir, 'messages', 'messages.properties')

    # Ensure the messages directory exists
    os.makedirs(os.path.dirname(properties_file), exist_ok=True)

    # Process files and collect results
    results = process_directory(templates_directory, properties_file)

    # Output the list of files and message counts
    for file_path, count in results:
        print(f"{file_path}: {count}")