import os
import re
import codecs

# Function to process HTML files and extract th:text="#{key}" messages
def process_html_file(file_path, properties_file, is_first_file):
    with codecs.open(file_path, 'r', 'utf-8') as infile:
        html_content = infile.read()

    # Regular expression to find all th:text="#{key}" messages, even in nested elements
    # This will capture th:text="#{key}" and extract the key and message
    pattern = re.compile(r'th:text="#\{([^}]+)\}"[^>]*>(.*?)</.*?>|th:text="#\{([^}]+)\}"[^>]*>([^<]+)<')
    matches = pattern.findall(html_content)

    # If no valid matches, return and print a message
    if not matches:
        print(f"Skipping {file_path}: No th:text=#{'{}'} found.")  # Escape curly braces
        return

    # Open the properties file in 'a' (append) mode for subsequent files, 'w' (overwrite) for the first file
    mode = 'w' if is_first_file else 'a'
    with codecs.open(properties_file, mode, 'utf-8') as prop_file:
        for match in matches:
            # The regex will return either the key or the text, check both possibilities
            key = match[0] or match[2]  # key is in the first or third group
            message = match[1] or match[3]  # message is in the second or fourth group

            # Strip leading and trailing whitespace from the message
            message = message.strip()

            # Write the key-message pair to the properties file
            prop_file.write(f"{key}={message}\n")
            # Print the key-message pair that is being written
            print(f"Key-Message Pair Written: {key}={message}")

    print(f"Processed: {file_path}")
    print(f"Messages saved to: {properties_file}")

# Function to process all HTML files in a directory and its subdirectories
def process_directory(directory, properties_file):
    is_first_file = True  # Flag to check if it's the first file being processed

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.html'):
                file_path = os.path.join(root, file)
                print(f"Processing HTML file: {file_path}")
                process_html_file(file_path, properties_file, is_first_file)
                is_first_file = False  # After processing the first file, switch to append mode

if __name__ == '__main__':
    # --- USER CONFIGURATION ---
    # Start from the src/main/resources directory
    base_dir = os.getcwd()  # Current working directory should be <project_root>/src/main/resources
    templates_directory = os.path.join(base_dir, 'templates')  # Path to your templates folder
    properties_file = os.path.join(base_dir, 'messages', 'messages.properties')  # Path to your messages properties file
    # --------------------------

    # Ensure the 'messages' directory exists, create it if not
    messages_dir = os.path.dirname(properties_file)
    if not os.path.exists(messages_dir):
        os.makedirs(messages_dir)

    # Start processing directly from the templates directory
    process_directory(templates_directory, properties_file)
    print("✅ Extraction completed, key-message pairs saved to properties file.")
