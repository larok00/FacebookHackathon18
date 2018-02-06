from flask import Blueprint, send_file, json, jsonify, request, Response
from flask import make_response
import cv2
import numpy as np
from io import BytesIO
from PIL import Image
import dlib
import sys
import os

curr_dir = os.path.dirname(os.path.realpath(__file__))

detector = dlib.get_frontal_face_detector()
predictor = dlib.shape_predictor(curr_dir + '/shape_predictor_68_face_landmarks.dat')

def getLandmarks(im):
    rects = detector(im, 1)
    
    if len(rects) > 1:
        pass
        #raise TooManyFaces
    if len(rects) == 0:
        pass
        #raise NoFaces

    h, w, c = im.shape
    #border = [(0,0), (0, h), (w,h), (w,0), (w//2, 0)]
    border = [(0,0), (0, h-1), (w-1,h-1), (w-1,0), (w//2, 0)]
        
    return [[(p.x, p.y) for p in predictor(im, rect).parts()] + border for rect in rects]

# main landmarks
myLandmarks = [0, 1, 6, 12, 16, 17, 19, 21, 22, 24, 26, 33, 36, 39, 42, 45, 48, 49, 51, 53, 54, 57]

# 48 - left corner of lips
# 54 - right corner of lips

landmarkTriangles = [
    # face
    [0, 4, 36], # 0
    [0, 17, 36],
    [4, 36, 48],
    [4, 8, 48],
    [8, 48, 57],
    [8, 54, 57],
    [8, 12, 54],
    [12, 16, 45],
    [12, 45, 54],
    [16, 26, 45],
    [17, 19, 36], # 10
    [19, 21, 39],
    [19, 36, 39],
    [21, 22, 33],
    [21, 33, 39],
    [22, 24, 42],
    [22, 33, 42],
    [24, 26, 45],
    [24, 42, 45],
    [36, 39, 48],
    [33, 39, 48], # 20
    #[33, 48, 51], old L
    [33, 42, 54],
    #[33, 51, 54], old R
    [42, 45, 54],
    [48, 51, 57],
    [51, 54, 57],
    
    # added for oldL
    [49, 33, 48],
    [49, 33, 51],
    [49, 48, 51],
    
    # added for oldR
    [53, 33, 51],
    [53, 33, 54],
    [53, 51, 54],
    
    #background
    [0, 19, 68], # the new one
    
    [68, 0, 69], # 28
    [69, 0, 4],
    [69, 4, 8],
    [69, 70, 8],
    [70, 8, 12],
    [70, 12, 16],
    [70, 16, 71],
    [71, 16, 26],
    [71, 24, 26],
    [68, 0, 17],
    [69, 17, 19],
    [68, 72, 19],
    [19, 21, 72],
    [72, 21, 22],
    [72, 22, 24],
    [71, 72, 24]    
]

def remapTriangle(img1, img2, tri1, tri2):
    # Find bounding box. 
    r1 = cv2.boundingRect(tri1)
    r2 = cv2.boundingRect(tri2)
    
    r1 = list(map(int, r1))
    r2 = list(map(int, r2))

    tri1Cropped = []
    tri2Cropped = []
    
    for i in range(0, 3):
        tri1Cropped.append(((tri1[i][0] - r1[0]),(tri1[i][1] - r1[1])))
        tri2Cropped.append(((tri2[i][0] - r2[0]),(tri2[i][1] - r2[1])))
    #print('tri2Cropped', tri2Cropped) 
    
    # Apply warpImage to small rectangular patches
    img1Cropped = img1[r1[1]:r1[1] + r1[3], r1[0]:r1[0] + r1[2]]

    # Given a pair of triangles, find the affine transform.
    warpMat = cv2.getAffineTransform( np.float32(tri1Cropped), np.float32(tri2Cropped) )

    # Apply the Affine Transform just found to the src image
    img2Cropped = cv2.warpAffine( img1Cropped, warpMat, (r2[2], r2[3]), None, flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT_101 )

    # Get mask by filling triangle
    mask = np.zeros((r2[3], r2[2], 3), dtype = np.uint8)
    cv2.fillConvexPoly(mask, np.int32(tri2Cropped), (1, 1, 1)) #, 16, 0);
    # Apply mask to cropped region
    img2Cropped = img2Cropped * mask

    # Copy triangular region of the rectangular patch to the output image
    #print(r2[3], r2[2])
    #print((img2[r2[1]:r2[1]+r2[3], r2[0]:r2[0]+r2[2]]).shape)
    #print(mask.shape)
    img2[r2[1]:r2[1]+r2[3], r2[0]:r2[0]+r2[2]] = img2[r2[1]:r2[1]+r2[3], r2[0]:r2[0]+r2[2]] * ( (1, 1, 1) - mask )

    img2[r2[1]:r2[1]+r2[3], r2[0]:r2[0]+r2[2]] = img2[r2[1]:r2[1]+r2[3], r2[0]:r2[0]+r2[2]] + img2Cropped
    

def remapImage(img, oldLandmarkPoints, newLandmarkPoints):
    if len(oldLandmarkPoints) != len(newLandmarkPoints):
        print('ERROR')
        return
    newImg = np.zeros(img.shape, np.uint8)
    for i in range(len(oldLandmarkPoints)):#[36:60]:
        x1, y1, z1 = oldLandmarkPoints[i]
        x2, y2, z2 = newLandmarkPoints[i]

        remapTriangle(img, newImg, np.array([x1,y1,z1]), np.array([x2,y2,z2]))

    return newImg

def smile(img, coeff):
    leftCorner = 48
    rightCorner = 54
    landmarks_list = getLandmarks(img)
    print(len(landmarks_list))
    for landmarks in landmarks_list:
        #print(landmarks)
        oldLandmarkPoints = []
        for t in landmarkTriangles:
            x,y,z = t
            p0 = landmarks[x]
            p1 = landmarks[y]
            p2 = landmarks[z]
            oldLandmarkPoints.append(np.array([p0, p1, p2]))
        newLandmarkPoints = []

        faceHeight = landmarks[8][1] - landmarks[33][1]
        faceWidth = landmarks[54][0] - landmarks[48][0]
        print(faceHeight)

        # move some points
        landmarks[36] = (landmarks[36][0], landmarks[36][1] - int(faceHeight / 40 * coeff))
        landmarks[45] = (landmarks[45][0], landmarks[45][1] - int(faceHeight / 40 * coeff))

        landmarks[19] = (landmarks[19][0], landmarks[19][1] - int(faceHeight / 30 * coeff))
        landmarks[24] = (landmarks[24][0], landmarks[24][1] - int(faceHeight / 30 * coeff))

        landmarks[48] = (landmarks[48][0] - abs(int(faceWidth / 7 * coeff)), landmarks[48][1] - int(faceHeight / 10 * coeff))
        landmarks[49] = (landmarks[49][0] - abs(int(faceWidth / 7 * coeff)), landmarks[49][1] - int(faceHeight / 30 * coeff))

        landmarks[54] = (landmarks[54][0] + abs(int(faceWidth / 7 * coeff)), landmarks[54][1] - int(faceHeight / 10 * coeff))
        landmarks[53] = (landmarks[53][0] + abs(int(faceWidth / 7 * coeff)), landmarks[53][1] - int(faceHeight / 30 * coeff))

        for t in landmarkTriangles:
            x,y,z = t
            p0 = landmarks[x]
            p1 = landmarks[y]
            p2 = landmarks[z]
            newLandmarkPoints.append(np.array([p0, p1, p2]))
        img = remapImage(img, oldLandmarkPoints, newLandmarkPoints)
    return img

def process_image_aux(img, coeff):
  img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
  img = smile(img, coeff)
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
    coeff = 0.8
    if 'emotion' in request.headers:
      if request.headers.get('emotion') == 'sad':
        coeff = -coeff
      elif request.headers.get('emotion') == 'vsad':
        coeff = -2 * coeff
      elif request.headers.get('emotion') == 'vhappy':
        coeff = 2 * coeff
    img = cv2.imdecode(data, color_image_flag)
    img = process_image_aux(img, coeff)
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
