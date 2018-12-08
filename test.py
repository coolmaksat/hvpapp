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
import platform

s=platform.python_version()
if "2.7" not in s:
	print("Please install Python version 2.7 and symlink the command [python] to Python 2.7 excutable.")
else:
	print("Hello World!")
