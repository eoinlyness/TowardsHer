#
# Author: Eoin Lyness
# Combine log files into single discretised dataset
#

import json
import copy
import os
import pandas as pd

#Retrieve name for each person
f = open('params/people.json')
people = json.load(f)
people = people.keys()

#Columns for output dataset
columns = ['user', 'activity', 'data', 'mood']
logData = []

#Find each file within folder
logs = [name for name in os.listdir('data')]
for fname in logs:
    if '.json' not in fname:
        continue
    
    #Load the json file and extract person from file name
    f = open('data/' + fname)
    temp = json.load(f)
    fname_parts = fname.split('_')
    p = fname_parts[2].replace('.json', '')

    #Iterate over each chunk
    for i in range(24):
        #Add chunk to dataset
        d = copy.deepcopy(temp[str(i)])
        a = d["activity"]
        m = d["mood"]
        logData.append([p,a,d,m])

logData = pd.DataFrame(logData, columns=columns)
logData.index.name = 'id'

#Create folder for output if does not already exist
if not os.path.exists('output'):
    os.mkdir('output')

#Output data to file
outputFile = "output/combined_data.csv"
logData.to_csv(outputFile)

