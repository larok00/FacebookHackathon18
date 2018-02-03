from flask import Blueprint, send_file, json, jsonify, request, Response
from flask import make_response
import cv2
import numpy as np
from io import BytesIO
from PIL import Image
import dlib
import sys


def process_image_aux(img):
  img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
  return img

#######################################################################

mod = Blueprint('process_image', __name__)

@mod.route("/processimage", methods=['POST'])
def process_image():
  if 'my_file' in request.files:
    photo = request.files['my_file']
    in_memory_file = BytesIO()
    photo.save(in_memory_file)
    data = np.fromstring(in_memory_file.getvalue(), dtype=np.uint8)
    color_image_flag = 1
    img = cv2.imdecode(data, color_image_flag)
    img = process_image_aux(img)
    print('processed image')
    img = Image.fromarray(img)
    out_memory_file = BytesIO()
    img.save(out_memory_file, format='jpeg')
    out_memory_file.seek(0)
    response = make_response(send_file(out_memory_file, mimetype='image/jpeg', attachment_filename='test_image.jpeg'))
    content_length = len(out_memory_file.getvalue())
    print(content_length)
    response.headers.add('content-length', str(content_length))
    return response

  # send black image
  data = np.zeros((100, 100, 3), dtype=np.uint8)
  f = BytesIO()
  np.save(f, data)
  im = Image.fromarray(data)
  im.convert('RGB')
  im.save('server/test_image.jpeg')
  return send_file('test_image.jpeg', mimetype='image/jpeg', attachment_filename='test_image.jpeg')
