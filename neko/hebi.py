from config import *
from utils.promptMaker import *
import pytchat
import openai
import json
import socket
from emoji import demojize
import threading
import re



# to help the CLI write unicode characters to the terminal
# sys.stdout = open(sys.stdout.fileno(), mode='w', encoding='utf8', buffering=1)

# use your own API Key, you can get it from https://openai.com/.
# I place my API Key in a separate file called config.py
openai.api_key = api_key

owner_name = 'Regis'

conversation = []
# Create a dictionary to hold the message data
history = {"history": conversation}

total_characters = 0


def openai_answer():
    global total_characters, conversation

    total_characters = sum(len(d['content']) for d in conversation)

    while total_characters > 4000:
        try:
            # print(total_characters)
            # print(len(conversation))
            conversation.pop(2)
            total_characters = sum(len(d['content']) for d in conversation)
        except Exception as e:
            print("Error removing old messages: {0}".format(e))

    with open("conversation.json", "w", encoding="utf-8") as f:
        # Write the message data to the file in JSON format
        json.dump(history, f, indent=4)

    prompt = getPrompt()

    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=prompt,
        max_tokens=128,
        temperature=1,
        top_p=0.9
    )
    message = response['choices'][0]['message']['content']
    conversation.append({'role': 'assistant', 'content': message})
    
    return message

if __name__ == '__main__':
    openai_answer()


########### AI #############################################################