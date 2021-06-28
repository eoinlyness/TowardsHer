#
# Author: Eoin Lyness
# Train collaborative filtering model (non-privacy-preserving)
#

import pandas as pd
import numpy as np
import sklearn
from fastai.tabular.all import *
from fastai.collab import *

seed = 4006
fname = 'output/cluster_out.csv'

#Load dataset and shuffle the rows
data = pd.read_csv(fname, index_col=0)
data = sklearn.utils.shuffle(data)

#Clear output file if it already exists
fname_out = 'output/collab_out.csv'
f = open(fname_out, "w")
f.truncate()
f.close()

#Create data loader from dataset
dls = CollabDataLoaders.from_df(data, user_name='user', item_name='cluster', rating_name='mood', bs=64, valid_pct=0.2, seed=seed)
#Create the model
#Output training and validation loss to file
learn = collab_learner(dls, y_range=(-1.5,1.5), wd=0.1, cbs=CSVLogger(fname=fname_out, append=True))
#Run the training
learn.fit_one_cycle(50)


#NOTE: Cannot save model with CSV logging enabled 
#Uncomment and run this code to retrain model without logging and export model to file

#learn = collab_learner(dls, y_range=(-1.5,1.5), wd=0.1)
#learn.fit_one_cycle(50)
#learn.export(fname='models/model.model')