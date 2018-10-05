import numpy as np
import pandas as pd
from sklearn.cross_validation import train_test_split

seed = 7
np.random.seed(seed)

############################First Split#################################
#read OMIM ids
all_omim = np.loadtxt("omim_all.txt", dtype='int')

#split by OMIMs into 5 lists for testing
np.random.shuffle(all_omim)
fold_1, fold_2, fold_3, fold_4, fold_5 = np.array_split(all_omim, 5)

#seperate training
#read PVP data and seperate instances to the 5 groups (training/testing sets)
#DOWNLOAD PVP_data at: "http://bio2vec.net/pvp/deepPVP/PVP_data.csv"
for i in range(5):
	j = i + 1
	f_split = open("fold" + str(j) + "/split.csv","w") 
	f_test = open("fold" + str(j) + "/testing.csv","w")
	f_train = open("fold" + str(j) + "/training.csv","w")
	f_val = open("fold" + str(j) + "/validation.csv","w")
	omim_test = eval("fold_"+str(j))
	omim_split = np.setdiff1d(all_omim, omim_test)
	np.savetxt("fold" + str(j) + "/omim_split.csv", omim_split, delimiter='\n', fmt='%i') 
	np.savetxt("fold" + str(j) + "/omim_testing.csv", omim_test, delimiter='\n', fmt='%i') 
	x_train , x_val = train_test_split(omim_split,test_size=0.2, random_state=seed)
	np.savetxt("fold" + str(j) + "/omim_train.csv", x_train, delimiter='\n', fmt='%i') 
	np.savetxt("fold" + str(j) + "/omim_val.csv", x_val, delimiter='\n', fmt='%i') 
	with open("PVP_data.csv") as f:
		header = next(f).strip()
		f_split.write(header+"\n")
		f_test.write(header+"\n")
		f_train.write(header+"\n")
		f_val.write(header+"\n")
		for line in f:
			omim_id = int(line.split(",")[8])
			if omim_id in eval("fold_"+str(j)):
				f_test.write(line)
			else:
				f_split.write(line)
				if omim_id in x_val:
					f_val.write(line)
				else:
					f_train.write(line)
	f_split.close()
	f_test.close()
	f_train.close()
	f_val.close()

############################Second Split#################################

#furthur split into train/validate

for x in range(5):
	y = x + 1
	#read OMIM ids
	all_omim = np.loadtxt("fold" + str(y) + "/omim_split.csv", dtype='int')
	#split by OMIMs into 5 lists for testing
	np.random.shuffle(all_omim)
	fold_1, fold_2, fold_3, fold_4, fold_5 = np.array_split(all_omim, 5)
	for i in range(5):
		j = i + 1
		f_split = open("fold" + str(y) + "/split_" + str(j) + ".csv","w") 
		f_test = open("fold" + str(y) + "/testing_" + str(j) + ".csv","w")
		f_train = open("fold" + str(y) + "/training_" + str(j) + ".csv","w")
		f_val = open("fold" + str(y) + "/validation_" + str(j) + ".csv","w")
		omim_test = eval("fold_"+str(y))
		omim_split = np.setdiff1d(all_omim, omim_test)
		np.savetxt("fold" + str(y) + "/omim_split_" + str(j) + ".csv", omim_split, delimiter='\n', fmt='%i') 
		np.savetxt("fold" + str(y) + "/omim_testing_" + str(j) + ".csv", omim_test, delimiter='\n', fmt='%i') 
		x_train , x_val = train_test_split(omim_split,test_size=0.2, random_state=seed)
		np.savetxt("fold" + str(y) + "/omim_train_" + str(j) + ".csv", x_train, delimiter='\n', fmt='%i') 
		np.savetxt("fold" + str(y) + "/omim_val_" + str(j) + ".csv", x_val, delimiter='\n', fmt='%i') 
		with open("fold" + str(y) + "/split.csv") as f:
			header = next(f).strip()
			f_split.write(header+"\n")
			f_test.write(header+"\n")
			f_train.write(header+"\n")
			f_val.write(header+"\n")
			for line in f:
				omim_id = int(line.split(",")[8])
				if omim_id in eval("fold_"+str(y)):
					f_test.write(line)
				else:
					f_split.write(line)
					if omim_id in x_val:
						f_val.write(line)
					else:
						f_train.write(line)
		f_split.close()
		f_test.close()
		f_train.close()
		f_val.close()

for x in range(5):
	y = x + 1
	df_train = pd.read_csv("fold" + str(y) + "/training.csv",low_memory=False)
	df_val = pd.read_csv("fold" + str(y) + "/validation.csv",low_memory=False)
	df_test = pd.read_csv("fold" + str(y) + "/testing.csv",low_memory=False)
	df_train.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
	df_val.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
	df_test.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
	df_train.to_csv("fold" + str(y) + "/training_data.csv", index = None, header = None)
	df_val.to_csv("fold" + str(y) + "/validation_data.csv", index = None, header = None)
	df_test.to_csv("fold" + str(y) + "/testing_data.csv", index = None, header = None)
	for i in range(5):
		j = i + 1
		df_train = pd.read_csv("fold" + str(y) + "/training_" + str(j) + ".csv",low_memory=False)
		df_val = pd.read_csv("fold" + str(y) + "/validation_" + str(j) + ".csv",low_memory=False)
		df_test = pd.read_csv("fold" + str(y) + "/testing_" + str(j) + ".csv",low_memory=False)
		df_train.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
		df_val.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
		df_test.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
		df_train.to_csv("fold" + str(y) + "/training_data_" + str(j) + ".csv", index = None, header = None)
		df_val.to_csv("fold" + str(y) + "/validation_data_" + str(j) + ".csv", index = None, header = None)
		df_test.to_csv("fold" + str(y) + "/testing_data_" + str(j) + ".csv", index = None, header = None)
		
