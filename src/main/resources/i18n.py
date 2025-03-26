import os
import re
import codecs

# Function to process HTML files and extract th:text="#{key}" messages
def process_html_file(file_path, properties_file, is_first_file):
    with codecs.open(file_path, 'r', 'utf-8') as infile:
        html_content = infile.read()

    # Regular expression to find all th:text="#{key}" messages, even in nested elements
    pattern = re.compile(r'th:text="#\{([^}]+)\}"[^>]*>(.*?)</.*?>|th:text="#\{([^}]+)\}"[^>]*>([^<]+)<')
    matches = pattern.findall(html_content)

    written_pairs = 0
    if matches:
        mode = 'w' if is_first_file else 'a'
        with codecs.open(properties_file, mode, 'utf-8') as prop_file:
            for match in matches:
                key = match[0] or match[2]  # Extract key
                message = match[1] or match[3]  # Extract message
                message = ' '.join(message.strip().splitlines())  # Convert multi-line to single-line
                
                prop_file.write(f"{key}={message}\n")
                written_pairs += 1

    return file_path, written_pairs  # Return file path and count of extracted messages

# Function to process all HTML files in a directory
def process_directory(directory, properties_file):
    is_first_file = True
    results = []

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.html'):
                file_path = os.path.join(root, file)
                result = process_html_file(file_path, properties_file, is_first_file)
                results.append(result)
                is_first_file = False

    return results

if __name__ == '__main__':
    base_dir = os.getcwd()
    templates_directory = os.path.join(base_dir, 'templates')
    properties_file = os.path.join(base_dir, 'messages', 'messages.properties')

    # Ensure the messages directory exists
    os.makedirs(os.path.dirname(properties_file), exist_ok=True)

    # Process files and collect results
    results = process_directory(templates_directory, properties_file)

    # Output the list of files and message counts, including files with zero messages
    for file_path, count in results:
        print(f"{file_path}: {count}")
