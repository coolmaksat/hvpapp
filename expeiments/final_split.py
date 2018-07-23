import numpy as np
import pandas as pd
from sklearn.cross_validation import train_test_split

seed = 7
np.random.seed(seed)

############################First Split#################################
#read OMIM ids
all_omim = np.loadtxt("omim_all.txt", dtype='int')

#read PVP data and seperate instances to the 5 groups (training/testing sets)

f_train = open("full-model/training.csv","w")
f_val = open("full-model/validation.csv","w")
x_train , x_val = train_test_split(all_omim,test_size=0.2, random_state=seed)
np.savetxt("full-model/omim_train.csv", x_train, delimiter='\n', fmt='%i') 
np.savetxt("full-model/omim_val.csv", x_val, delimiter='\n', fmt='%i') 
with open("PVP_data.csv") as f:
	header = next(f).strip()
	f_train.write(header+"\n")
	f_val.write(header+"\n")
	for line in f:
		omim_id = int(line.split(",")[8])
		if omim_id in x_val:
			f_val.write(line)
		else:
			f_train.write(line)
	f_train.close()
	f_val.close()

df_train = pd.read_csv("full-model/training.csv",low_memory=False)
df_val = pd.read_csv("full-model/validation.csv",low_memory=False)
df_train.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
df_val.drop(['Chr', 'Position', 'RSID', 'Ref', 'Alt' , 'Gene' , 'OMIM'], axis =1, inplace = True)
df_train.to_csv("full-model/training_data.csv", index = None, header = None)
df_val.to_csv("full-model/validation_data.csv", index = None, header = None)

