from flask import Blueprint, send_file
import cv2
import numpy as np
from io import BytesIO
from PIL import Image
import dlib

mod = Blueprint('process_image', __name__)

@mod.route("/processimage", methods=['POST'])
def process_image():
  data = np.zeros((100, 100, 3), dtype=np.uint8)
  f = BytesIO()
  np.save(f, data)
  im = Image.fromarray(data)
  im.convert('RGB')
  im.save('server/test_image.jpeg')
  return send_file('test_image.jpeg', mimetype='image/jpeg', attachment_filename='test_image.jpeg')
