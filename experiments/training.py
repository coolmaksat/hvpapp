import keras
from keras.layers import Dropout
from keras.models import Sequential
from keras.layers import Dense
import numpy as np
import pandas as pd
import sys
import h5py

seed = 7
np.random.seed(seed)

def data():
	train_file = "training_data.csv" 
	val_file = "validation_data.csv" 
	train_data = np.loadtxt(train_file, delimiter=",")
	val_data = np.loadtxt(val_file, delimiter=",")
	# split into input (X) and output (Y) variables
	X_train = train_data[:,1:68]
	Y_train = train_data[:,0]
	X_val = val_data[:,1:68]
	Y_val = val_data[:,0]
	return X_train, Y_train, X_val, Y_val

#Train and save the model
X_train, Y_train, X_val, Y_val = data()

model = Sequential()
model.add(Dense(67, input_dim=67, kernel_initializer='uniform', activation='relu'))
model.add(Dropout(0.2))
model.add(Dense(32, kernel_initializer='uniform', activation='relu'))
model.add(Dropout(0.2))
model.add(Dense(256, kernel_initializer='uniform', activation='relu'))
model.add(Dropout(0.2))
model.add(Dense(1, kernel_initializer='uniform', activation='sigmoid'))

adam = keras.optimizers.Adam(lr=0.001)
model.compile(loss='binary_crossentropy', optimizer=adam, metrics=['accuracy'])

model.fit(X_train, Y_train, validation_data=(X_val,Y_val), batch_size=2500, nb_epoch=100, verbose=2)
model.save("final_model.hdf5")
