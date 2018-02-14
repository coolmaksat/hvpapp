import keras
from keras.layers import Dropout
from keras.models import Sequential
from keras.layers import Dense
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os
import h5py

## Training Model

# "modelname" should be '.hdf5' file
def Train_DNN(modelname, train_file):
    seed = 7
    np.random.seed(seed)
    dataset = np.loadtxt(train_file, delimiter=",")

    # split into input (X) and output (Y) variables
    X = dataset[:,1:68]
    Y = dataset[:,0]

    # create model
    model = Sequential()

    # model.add(input_shape=(80,))
    model.add(Dense(67, input_dim=67,  kernel_initializer='uniform', activation='relu'))
    model.add(Dropout(0.2))
    model.add(Dense(134, kernel_initializer='uniform', activation='relu'))
    model.add(Dropout(0.4))
    model.add(Dense(67, kernel_initializer='uniform', activation='relu'))
    model.add(Dropout(0.2))
    model.add(Dense(1, kernel_initializer='uniform', activation='sigmoid'))

    # Compile model
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    from keras.callbacks import ModelCheckpoint

    ## Model Name
    filepath= modelname
    checkpoint = ModelCheckpoint(filepath, monitor='val_loss', verbose=0, save_best_only=True, mode='min')
    callbacks_list = [checkpoint]

    # Fit the model
    history = model.fit(X, Y, validation_split=.20, epochs=300, batch_size=20000, callbacks = callbacks_list, verbose=1, initial_epoch=0)
    # list all data in history
    print(history.history.keys())
    
    plt.plot(history.history['acc'])
    plt.plot(history.history['val_acc'])
    plt.title('Model accuracy')
    plt.ylabel('Accuracy')
    plt.xlabel('epoch')
    plt.legend(['Training', 'Validation'], loc='lower right')
    #plt.show()
    plt.savefig('model_no_brca_accuracy.png', bbox_inches='tight')
    plt.clf()
    plt.cla()
    plt.close()
    # summarize history for loss
    plt.plot(history.history['loss'])
    plt.plot(history.history['val_loss'])
    plt.title('Model loss')
    plt.ylabel('Loss')
    plt.xlabel('epoch')
    plt.legend(['Training', 'Validation'], loc='upper right')
    #plt.show()
    plt.savefig('model_no_brca_loss.png', bbox_inches='tight')
    
    
    
# Train Model
#Train_DNN("model.hdf5", "PVP_feature.csv")    
Train_DNN("model.hdf5", "PVP_feature.csv")

print("Training Complete!!!")

