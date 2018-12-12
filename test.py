import sys
s=sys.version_info[0]

if s > 2:
	print("Incompatible python version. Please install python 2.7")
else:
	import keras
	from keras.layers import Dropout
	from keras.models import Sequential
	from keras.layers import Dense
	import numpy as np
	import pandas as pd
	import os
	import sys
	import h5py
	import tensorflow as tf

	print("Required python dependencies found.")
