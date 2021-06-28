#
# Author: Eoin Lyness
# Discretise log file into 1 hour chunks 
#

import json
import sys
import getopt

#Check args
argv = sys.argv[1:]

inputFile = ''
outputFile = ''

if len(argv) < 2:
    print('expected args: -i <inputFile> -o <outputFile>')
    sys.exit(2)

try:
    opts, args = getopt.getopt(argv,"hi:o:",["ifile=","ofile="])
except getopt.GetoptError:
    print('expected args: -i <inputFile> -o <outputFile>')
    sys.exit(2)
for opt, arg in opts:
    if opt in ("-i", "--ifile"):
        inputFile = arg
    elif opt in ("-o", "--ofile"):
        outputFile = arg
    else:
        print('expected args: -i <inputFile> -o <outputFile>')
        sys.exit()


f = open(inputFile)
template = json.load(f)

periods = {}

#Setup structure
for i in range(24):
    periods[str(i)] = {}
    for key in template.keys():
        periods[str(i)][key] = []

#Iterate over each section of log
for item in template.items():
    #Iterate over each entry in the section
    for sub_item in item[1]:
        #Extract hour from log entry
        period = sub_item["time"][0:2]
        #Add activity to relevant chunk
        periods[period][item[0]].append(sub_item)

#Output to json file
f = open(outputFile, 'w')
json.dump(periods, f, indent=4)

