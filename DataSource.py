import json
import nltk
from nltk.tokenize import sent_tokenize
import os

# Download the punkt tokenizer models
# nltk.download('punkt')

# Function to process a single JSON file
def process_json_file(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        articles = json.load(f)

    processed_text = []

    for article in articles:
        sentences = sent_tokenize(article['text'])
        processed_text.extend(sentences)

    # Ensure the output directory exists
    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    with open(output_file, 'w', encoding='utf-8') as f:
        for sentence in processed_text:
            f.write(sentence + '\n')

# Example usage
input_file = '/Users/qingdamai/Documents/25 Winter/CS  451:CS 651 Data-Intensive Distributed Computing/DistributedSys/data/wiki_sample.json'
output_file = 'result/wiki_sample_processed.txt'
process_json_file(input_file, output_file)