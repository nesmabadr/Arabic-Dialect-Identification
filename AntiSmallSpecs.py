import os
import cv2
from imutils import paths

for imagePath in paths.list_images('./train'):
	# load the image, convert it to grayscale, and describe it
	image = cv2.imread(imagePath)
	height, width = image.shape[:2]
	if width != 944 or height !=591:
		os.remove(imagePath)
