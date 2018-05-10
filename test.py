from keras import applications
from keras.layers import Dense, Dropout
from keras.optimizers import Adam
from keras.preprocessing.image import ImageDataGenerator
from keras.models import Model
from keras.callbacks import ReduceLROnPlateau, ModelCheckpoint, TensorBoard
import numpy as np
from keras.preprocessing.image import img_to_array, load_img
import numpy as np
import tensorflow as tf
import os
import time
from keras.applications.mobilenet import preprocess_input
import tensorflow as tf
import os
import scipy.misc


os.environ["CUDA_VISIBLE_DEVICES"]="2"


#initial_model = applications.inception_v3.InceptionV3(include_top=False, weights='imagenet', input_tensor=None, input_shape=(224, 224, 3), pooling='max', classes=4)
initial_model = applications.mobilenet.MobileNet(input_shape=(224,224, 3), alpha=1.0, depth_multiplier=1, dropout=0, include_top=False, weights='imagenet', input_tensor=None, pooling='max', classes=4)

pred = Dense(4, activation = 'softmax')(initial_model.output)
model = Model(initial_model.input, pred)

model.compile(optimizer = Adam(lr = 1e-4), loss = 'categorical_crossentropy', metrics = ['acc'])

'''trainingDataGenerator =  ImageDataGenerator(
    featurewise_center=False,

    samplewise_center=False,
    featurewise_std_normalization=False,
    samplewise_std_normalization=False,
    zca_whitening=False,
    rotation_range=10,
    width_shift_range=0.2,
    height_shift_range=0.2,
    horizontal_flip=True,
    vertical_flip=False,
    zoom_range=0.2)


trainingDataGenerator = trainingDataGenerator.flow_from_directory('TrainPreprocessed/',class_mode='categorical',  batch_size = 16, target_size=(224,224))
'''


testingDataGenerator = ImageDataGenerator(featurewise_center=False, featurewise_std_normalization=False, zca_whitening=False)

#testingDataGenerator = testingDataGenerator.flow_from_directory('Test2Preprocessed/', class_mode='categorical', target_size=(224,224), batch_size = 1, shuffle = False)



model.load_weights ('weights_1mn.h5')

x_test = img_to_array (load_img ("Test2Preprocessed/NOR/NOR000082.wav.png", target_size= (224, 224)))
x_test = np.expand_dims(x_test, axis = 0)
#scipy.misc.toimage (np.squeeze(x_test), cmin= 0.0, cmax=None).save("Test2Preprocessed/EGY/abl.png")
x_test = preprocess_input(x_test)
#scipy.misc.toimage (np.squeeze(x_test), cmin= 0.0, cmax=None).save("Test2Preprocessed/EGY/ba3d.png")

#test_accuracy = model.evaluate_generator(testingDataGenerator, steps=3)
#print ("Testing Accuracy", test_accuracy[1])

predictions = model.predict(x_test, steps = 1)
predictions = np.argmax(predictions, axis = -1)


label_map = {'EGY' : 0, 'GLF' : 1, 'LAV' : 2, 'NOR' : 3}
#label_map = (trainingDataGenerator.class_indices)
label_map = dict((v, k) for k, v in label_map.items())
predictions = [label_map[k] for k in predictions]



####Inception: Accuracy: 92%
####Mobilenet: Accuracy : 89%




print ('Predictions:', predictions)
