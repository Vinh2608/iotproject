import time
import  sys
from  Adafruit_IO import  MQTTClient

# import pywhatkit as what
import os

from config import *

# from utils.promptMaker import *
# import pytchat
# import openai
# import json
# import socket
# from emoji import demojize
# import threading
# import re

import json
import re

import hebi
######## IMPORT #############################




########### AI #############################################################

temp = 'there is nothing to feel'
light = 'there is just an empty void'
ai = 'an AI'

stranger = False

AIO_FEED_ID = kitsune_feed_id
# 'den', 'quat'
AIO_USERNAME = aio_user
AIO_KEY = aiot_key


def  connected(client):
    print(f'Ket noi thanh cong...')
    for top in AIO_FEED_ID:
        client.subscribe(top)

def  subscribe(client , userdata , mid , granted_qos):
    print(f'Subcribe thanh cong...')

def  disconnected(client):
    print("Ngat ket noi...")
    sys.exit(1)

# ser = None

def  message(client , feed_id , payload):
    print(f"Nhan du lieu from {feed_id} with {payload}")
    if feed_id == 'cambien-nhiet':
        global temp
        temp = payload
        print(f'temp update: {temp}')
    if feed_id == 'cambien-as':
        global light
        light = payload
        print(f'light update: {light}')
    if feed_id == 'ai':
        global ai
        ai = payload
        print(f'ai update: {payload}')
        if payload == 'strangers':
            # speak('warning, camera detects a stranger')
            stranger = True
            
            

client = MQTTClient(AIO_USERNAME , AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = message
client.on_subscribe = subscribe
client.connect()
client.loop_background()




####### YOUR SIRI ##########################################################
# sub_id: cambien-nhiet, cambien-as -- get from app (app get from ada)
# send_id: den, quat -- ai send to app, app send to ada


def match(text):
    f = open('training.json', 'r')
    
    data = json.load(f)
    
    query = data['query']
    chat = data['chat']
    ctrl = data['ctrl']
    ai = data['ai']
    
    exactMatch = re.compile(r'\b%s\b' % '\\b|\\b'.join(query), 
                            flags=re.IGNORECASE)
    q = (exactMatch.findall(text))
    # print(q)
    
    exactMatch = re.compile(r'\b%s\b' % '\\b|\\b'.join(chat), 
                            flags=re.IGNORECASE)
    ch = (exactMatch.findall(text))
    # print(ch)
    
    exactMatch = re.compile(r'\b%s\b' % '\\b|\\b'.join(ctrl), 
                            flags=re.IGNORECASE)
    ct = (exactMatch.findall(text))
    # print(ct)
    
    exactMatch = re.compile(r'\b%s\b' % '\\b|\\b'.join(ai), 
                            flags=re.IGNORECASE)
    a = (exactMatch.findall(text))
    # print(a)
    
    return len(q), len(ch), len(ct), len(a)



def Usagi(user, req):
    
    q, ch, ct, cam = match(req)
    
    do = max([q, ch, ct])
    
    if do == 0:
        do = -1
    
    # print(f'q = {q}, ch = {ch}, ct = {ct}, max ={do}')

    if q == do:
        # query info from ada
        
        if "temperature" in req.lower():
            # temp = random.randint(0, 40)
            global temp
            msg = (f'the temperature now is {temp} Celsius')
            # speak(msg)
            # get tmp
            print(msg)
            user.sendall(msg.encode())
            
        if "light" in req.lower():
            # get light_meter
            # light = random.randint(0, 100)
            global light
            msg = (f'the light now is {light} lux')
            # speak(msg)
            print(msg)
            user.sendall(msg.encode())

        if cam != 0:
            # get recognition
            global ai
            msg = (f'face recognition is {ai}')
            # speak(msg)
            print(msg)
            user.sendall(msg.encode())
       
    
    elif ct == do:
        #ctrl
        if "light" in req.lower():
            signal = None
            msg = None
            if "on" in req.lower():
                signal = 'L'
                msg = "let there be light"
                
            if "off" in req.lower():
                switch = 'off'
                signal = 'l'
                msg = 'there only darkness in this world'
                    
                
            client.publish('den', signal)
            print(req)
            # speak(msg)
            print(msg)
            user.sendall(msg.encode())
                
            
        if "fan" in req.lower():
            signal = None
            msg = None
            if "on" in req.lower():
                signal = 100
                msg = "fan on"
                
            if "off" in req.lower():
                signal = 0
                msg = "fan off"
                
                
            if ("level" or "mode") and "zero" in req.lower():
                signal = 0
                msg = "fan off"
                
            if ("level" or "mode") and "one" in req.lower():
                signal = 25
                msg = "level one"
                
                
            if ("level" or "mode") and "two" in req.lower():
                signal = 50
                msg = "level two"
                
                
            if ("level" or "mode") and "three" in req.lower():
                signal = 75
                msg = "level three"
                
                
            if ("level" or "mode") and ("four" or "max") in req.lower():
                signal = 100
                msg = "level max"
                
                
            client.publish("quat", signal)
            print(req)
            # speak(msg)
            print(msg)
            user.sendall(msg.encode())
            
    else:
         # chat gpt
        chat = True
        while chat:
            # conv = input('text here: ')
            conv = user.recv(BUFFER_SIZE).decode()
            if "terminate chat" and ('turn off' or 'log out') in conv:
                msg = "see you another time"
                # speak(msg)
                print(msg)
                os.remove('conversation.json')
                chat = False
            else:
                result = "Regis said " + conv
                hebi.conversation.append({'role': 'user', 'content': result})
                msg = hebi.openai_answer()
                # speak(msg)
                print(msg)
                user.sendall(msg.encode())
        # pass
        
            
# if True:
#     msg = ('welcome master')
#     speak(msg)
# while True:
#     req = input('your req is: ')
#     # req = get_audio()
#     if req:
#         Usagi(req)


######## SERVER #################################################

import socket
import threading
import tqdm


HOST = '127.0.0.1'
#can use any port between 0-65535
PORT = 32664
#maximum num of client can connect to server
# LISTENER_LIM = 10
active = []	# list of all currently active user
#inside have username, host, port in this order

HEADER = 2048

BUFFER_SIZE = 4096



# func to handle client
def client_handler(user, addr):
	
	#server listen for cli mess
	#contain username
 
    while True:
        # try:
        #     req = user.recv(BUFFER_SIZE).decode()
        #     print(f'req: {req}')
        #     global stranger
        #     if stranger:
        #         print('warning, stranger dectected')
        #     if req != '':
        #         Usagi(user, req)
            
        # except:
        #     print('Error')
        #     return
        
        req = user.recv(BUFFER_SIZE).decode()
        print(f'req: {req}')
        global stranger
        if stranger:
            print('warning, stranger dectected')
        if req != '':
            Usagi(user, req)
        
           

			
	# listen_for_mess(client, username)







# __main__
# if __name__ == '__main__':
active.clear()
#creating socket class obj:
server = socket.socket(socket.AF_INET,
    socket.SOCK_STREAM)
#not work -> change to DGRAM
#AF_INET: stating of using IPv4 addresses
#SOCK_STREAM: using TCP packets for comm.
#for UDP, use SOCK_DGRAM


#bind server to a hosting port
    #creating try-catch block
try:
    #provide server with address in the form host IP
    # & port
    server.bind((HOST, PORT))
    print(f"connected to host {HOST}, {PORT}")
except:
    print(f"Unable to bind to host {HOST} and port {PORT}")
#set server limit
server.listen()


#while loop to listen to client connections
while True:

    user, address = server.accept()
    # accept func waits for new connection, return
    # a new socket represent connection $ client address
    # client: socket of client
    # address: address of client
# 		print(f"new user at host {address[0]}\
# port {address[1]} joined the chat")


    threading.Thread(target=client_handler,
        args=(user, address, )).start()
