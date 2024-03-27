import serial.tools.list_ports
import random
import time
import  sys
from  Adafruit_IO import  MQTTClient

AIO_FEED_ID = ['den', 'quat']
AIO_USERNAME = "RegL"
AIO_KEY = "aio_OdjH53bqgD9kUnBm30qSLyFPHraT"

def connected(client):
    for top in AIO_FEED_ID:
        print(f'Ket noi thanh cong toi {top}')
        client.subscribe(top)

def  subscribe(client , userdata , mid , granted_qos):
    print(f'Subcribe thanh cong...')

def  disconnected(client):
    print("Ngat ket noi...")
    sys.exit(1)

ser = None

def  message(client , feed_id , payload):
    print(f"Nhan du lieu from {feed_id} with {payload}")
    # ser.write((str(payload)).encode())

client = MQTTClient(AIO_USERNAME , AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = message
client.on_subscribe = subscribe
client.connect()
client.loop_background()
######## END MAIN(1) #########

############ UART ##############
# def getPort():
#     ports = serial.tools.list_ports.comports()
#     N = len(ports)
#     print(f'# of ports: {N}')
#     commPort = "None"
#     for i in range(0, N):
#         port = ports[i]
#         strPort = str(port)
#         print(f'port name: {strPort}')
#         if "CH340" in strPort:
#             print(strPort)
#             splitPort = strPort.split(" ")
#             commPort = (splitPort[0])
#     print(f'connected to {commPort}')
#     return commPort

# Port = getPort()

# if Port != "None":
#     print(f'get {Port}')
#     ser = serial.Serial( port=Port, baudrate=115200)
#     print(ser)

# mess = ""
# def processData(data):
#     data = data.replace("#", "")
#     data = data.replace("!", "")
#     splitData = data.split(":")
#     print(splitData)
#     if splitData[0] == "t":
#         client.publish("cambien-nhiet", splitData[1]) # update temp
#     if splitData[0] == "as":
#         client.publish("cambien-as", splitData[1]) # light sensor
    

# mess = ""
# # ser.write('helloooooo'.encode())
# def readSerial():
#     # print('hrlo')
#     # print(ser)
#     bytesToRead = ser.inWaiting()
#     print(bytesToRead)
#     # print('jllo')
#     if (bytesToRead > 0):
#         global mess
#         mess = mess + ser.read(bytesToRead).decode("UTF-8")
#         print(mess)
#         while ("!" in mess) and ("#" in mess):
#             print('more mess')
#             start = mess.find("!")
#             end = mess.find("#")
#             processData(mess[start:end + 1])
#             if (end == len(mess)):
#                 mess = ""
#             else:
#                 mess = mess[end+1:]
########## UART ##############

#### MAIN ########
while True:
    # readSerial()
    light = random.randint(0, 100)
    temp = random.randint(0, 40)
    print("The light is: ", light)
    print("The temperature is: ", temp)
    client.publish("cambien-as", light) # update light
    client.publish("cambien-nhiet", temp) # update temp
    time.sleep(10)