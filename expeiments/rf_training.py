import numpy as np
import pandas as pd
import sys
import h5py
from sklearn.ensemble import RandomForestClassifier
import pickle
from sklearn.externals import joblib

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

# Instantiate model with 100 decision trees
rf =  RandomForestClassifier(n_estimators=100, max_features=6 )
rf.fit(X_train, Y_train);
joblib.dump(rf, "model_rf.pkl") 
