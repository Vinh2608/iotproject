import face_recognition
import os, sys
import cv2
import numpy as np
import math
from  Adafruit_IO import  MQTTClient
import serial.tools.list_ports
import base64
from io import BytesIO

AIO_FEED_ID = ['cambien-nhiet', 'cambien-as', 'ai', 'den', 'quat', 'image']
AIO_USERNAME = "RegL"
AIO_KEY = "aio_luIs92FDIivtMiO1A62esa34l7yD"

def connected(client):
    print("Ket noi thanh cong...")
    for feed_id in AIO_FEED_ID:
        client.subscribe(feed_id)

def subscribe(client , userdata , mid , granted_qos):
    print("Subcribe thanh cong...")

def disconnected(client):
    print("Ngat ket noi...")
    sys.exit (1)

def message(client , feed_id , payload):
    pass

client = MQTTClient(AIO_USERNAME , AIO_KEY)
client.on_connect = connected
client.on_disconnect = disconnected
client.on_message = message
client.on_subscribe = subscribe
client.connect()
client.loop_background()

# Helper
def face_confidence(face_distance, face_match_threshold=0.5):
    range = (1.0 - face_match_threshold)
    linear_val = (1.0 - face_distance) / (range * 2.0)

    if face_distance > face_match_threshold:
        return str(round(linear_val * 100, 2)) + '%'
    else:
        value = (linear_val + ((1.0 - linear_val) * math.pow((linear_val - 0.5) * 2, 0.2))) * 100
        return str(round(value, 2)) + '%'


class FaceRecognition:
    face_locations = []
    face_encodings = []
    face_names = []
    known_face_encodings = []
    known_face_names = []
    process_current_frame = True

    def __init__(self):
        self.encode_faces()

    def encode_faces(self):
        for image in os.listdir('faces'):
            face_image = face_recognition.load_image_file(f"faces/{image}")
            face_encoding = face_recognition.face_encodings(face_image)[0]
            
            self.known_face_encodings.append(face_encoding)
            self.known_face_names.append(image)

    def run_recognition(self):
        video_capture = cv2.VideoCapture(0)

        if not video_capture.isOpened():
            sys.exit('Video source not found...')

        counter = 5
        while True:
            ret, frame = video_capture.read()

            # Only process every other frame of video to save time
            if self.process_current_frame:
                # Resize frame of video to 1/4 size for faster face recognition processing
                small_frame = cv2.resize(frame, (0, 0), fx=0.25, fy=0.25)

                # Convert the image from BGR color (which OpenCV uses) to RGB color (which face_recognition uses)
                rgb_small_frame = small_frame[:, :, ::-1]

                # Find all the faces and face encodings in the current frame of video
                self.face_locations = face_recognition.face_locations(rgb_small_frame)
                self.face_encodings = face_recognition.face_encodings(rgb_small_frame, self.face_locations)

                self.face_names = []
                for face_encoding in self.face_encodings:
                    # See if the face is a match for the known face(s)
                    matches = face_recognition.compare_faces(self.known_face_encodings, face_encoding)
                    name = "Unknown"
                    confidence = '???'

                    # Calculate the shortest distance to face
                    face_distances = face_recognition.face_distance(self.known_face_encodings, face_encoding)
                    best_match_index = np.argmin(face_distances)
                    if face_distances[best_match_index] > 0.6:
                        pass
                    elif matches[best_match_index]:
                        confidence = face_confidence(face_distances[best_match_index])
                        if float(confidence.split('%')[0]) < 80:
                            pass
                        else:
                            name = self.known_face_names[best_match_index]
                        
                    self.face_names.append(f'{name} ({confidence})')

            self.process_current_frame = not self.process_current_frame

            # Display the results
            for (top, right, bottom, left), name in zip(self.face_locations, self.face_names):
                # Scale back up face locations since the frame we detected in was scaled to 1/4 size
                top *= 4
                right *= 4
                bottom *= 4
                left *= 4


                # Create the frame with the name
                cv2.rectangle(frame, (left, top), (right, bottom), (0, 0, 255), 2)
                #cv2.rectangle(frame, (left, bottom - 35), (right, bottom), (0, 0, 255), cv2.FILLED)
                cv2.putText(frame, name, (left + 6, bottom - 6), cv2.FONT_HERSHEY_DUPLEX, 0.8, (255, 255, 255), 1)
                
                if name.split()[0] == "Unknown":
                    _, pushFrame = cv2.imencode('.jpg', frame[top:bottom, left:right])
                    data = base64.b64encode(pushFrame)

                    if len(data) > 102400:
                        print('Image is too big')
                        break
                    else:
                        counter -= 1
                        if counter < 0:
                            counter = 5
                            print('Publish image')
                            client.publish("ai", name.split()[0].split('.')[0])
                            client.publish("image", data)

            # Display the resulting image
            cv2.imshow('Face Recognition', frame)

            # _, pushFrame = cv2.imencode('.jpg', frame)
            # data = base64.b64encode(pushFrame)
            
            # if len(data) > 102400:
            #     print('Image is too big')
            #     break
            # else:
            #     print('Publish image')
            # counter -= 1
            # if counter < 0:
            #     counter = 5
            #     client.publish("ai", labels[index])
            #     client.publish("image", data)

            # Hit 'q' on the keyboard to quit!
            if cv2.waitKey(1) == ord('q'):
                break

        # Release handle to the webcam
        video_capture.release()
        cv2.destroyAllWindows()


if __name__ == '__main__':
    fr = FaceRecognition()
    fr.run_recognition()
