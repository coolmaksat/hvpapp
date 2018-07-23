import keras
from keras.layers import Dropout
from keras.models import Sequential
from keras.layers import Dense
import numpy as numpy
import sys
import h5py
from hyperopt import Trials, STATUS_OK, tpe
from hyperas import optim
from hyperas.distributions import choice, uniform
from hyperopt import space_eval
from hyperas.utils import eval_hyperopt_space


seed = 7
numpy.random.seed(seed)

def data():
	i = int(sys.argv[1])
	j = int(sys.argv[2])
	train_file = "fold" + str(i) + "/training_data_" + str(j) + ".csv" 
	val_file = "fold" + str(i) + "/validation_data_" + str(j) + ".csv" 
	train_data = numpy.loadtxt(train_file, delimiter=",")
	val_data = numpy.loadtxt(val_file, delimiter=",")
	# split into input (X) and output (Y) variables
	X_train = train_data[:,1:68]
	Y_train = train_data[:,0]
	X_val = val_data[:,1:68]
	Y_val = val_data[:,0]
	return X_train, Y_train, X_val, Y_val

def model(X_train, Y_train, X_val, Y_val):    
	model = Sequential()
	model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, input_dim=67, kernel_initializer='uniform', activation='relu'))
	model.add(Dropout(0.2))
	layers = {{choice(['two', 'three', 'four'])}}
	if layers == 'two':
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))
	elif layers == 'three':
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))
	else:
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))
		model.add(Dense({{choice([32, 64, 67, 128, 134, 201, 256, 512])}}, kernel_initializer='uniform', activation='relu'))
		model.add(Dropout(0.2))

	model.add(Dense(1, kernel_initializer='uniform', activation='sigmoid'))

	adam = keras.optimizers.Adam(lr=0.001)
	model.compile(loss='binary_crossentropy', optimizer=adam, metrics=['accuracy'])

	model.fit(X_train, Y_train, validation_data=(X_val,Y_val), batch_size={{choice([2500, 5000,10000, 150000, 20000])}}, nb_epoch=50, verbose=2)

	score, acc = model.evaluate(X_val, Y_val, verbose=0)
	print('Validation accuracy:', acc)
	return {'loss': -acc, 'status': STATUS_OK, 'model': model}

X_train, Y_train, X_val, Y_val = data()
trials = Trials()
best_run, best_model, space = optim.minimize(model=model, data=data, algo=tpe.suggest, max_evals=50, trials=trials, eval_space=True, return_space=True)
print "Best model:"
print(best_run) 
fout = open("report-folds-extra.txt","a") 
fout.write("Best performing model chosen hyper-parameters for Fold " + str(sys.argv[1]) + " sub_fold " +  str(sys.argv[2])  + "\n")
fout.write(str(best_run))
fout.write("\n")
fout.write("*****************************************\n")
fout.close()
