import numpy as np
from cv2 import cv2
import face_recognition
import base64


def main(bitmapOri, bitmapTest):
    decoded_data = base64.b64decode(bitmapOri)
    np_data = np.fromstring(decoded_data, np.uint8)
    imgOri = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)

    decoded_data = base64.b64decode(bitmapTest)
    np_data = np.fromstring(decoded_data, np.uint8)
    imgTest = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)

    imgOri = cv2.cvtColor(imgOri, cv2.COLOR_BGR2RGB)
    imgTest = cv2.cvtColor(imgTest, cv2.COLOR_BGR2RGB)

    # faceLocOri = face_recognition.face_locations(imgOri)[0]
    encodeOri = face_recognition.face_encodings(imgOri)[0]

    # faceLocTest = face_recognition.face_locations(imgTest)[0]
    encodeTest = face_recognition.face_encodings(imgTest)[0]

    results = face_recognition.compare_faces([encodeOri], encodeTest)

    if(results == [True]):
        return 1
    else:
        return 0
