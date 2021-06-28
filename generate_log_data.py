#
# Author: Eoin Lyness
# Generate synthetic log files
#

import json
import os
import random
import copy

#Initial declarations
numLogs = 1000
currentPerson = 0
currentMood = 0
templates = []
variables = {}
people = {}
log = {}
random.seed(4006)

########################
# FUNCTION DEFINITIONS #
########################

def generateApps(i):
    global currentMood
    for app in log[str(i)]["appInfo"]:
        appID = app["activity"]
        r = random.randint(0,len(variables["apps"][str(appID)])-1)
        selectedApp = variables["apps"][str(appID)][r]
        app["applicationName"] = selectedApp["applicationName"]
        app["packageName"] = selectedApp["packageName"]

        currentMood = currentMood + people[currentPerson][appID]

def generateCalls(i):
    global currentMood
    for call in log[str(i)]["callList"]:
        personID = call["id"]
        r = random.randint(0,len(people[currentPerson][str(personID)])-1)
        selectedPerson = people[currentPerson][str(personID)][r]
        call["name"] = selectedPerson["name"]
        call["number"] = selectedPerson["number"]

        currentMood = currentMood + selectedPerson["mood"]

def generateTexts(i,smsType):
    global currentMood
    for sms in log[str(i)][smsType]:
        personID = sms["id"]
        r = random.randint(0,len(people[currentPerson][str(personID)])-1)
        selectedPerson = people[currentPerson][str(personID)][r]
        sms["name"] = selectedPerson["name"]
        sms["from"] = selectedPerson["number"]

        currentMood = currentMood + selectedPerson["mood"]

def generateLocations(i):
    global currentMood
    x = y = 0
    for location in log[str(i)]["path"]:
        locationID = location["activity"]
        selectedLocation = {}
        if locationID == "11":
            selectedLocation = variables["addresses"][locationID][0]
        else:
            selectedLocation = variables["addresses"][locationID][y]
            y = y + 1

        log[str(i)]["places"][x]["address"] = selectedLocation["address"]
        location["latitude"] = selectedLocation["latitude"]
        location["longitude"] = selectedLocation["longitude"]

        x = x + 1

        currentMood = currentMood + people[currentPerson][str(locationID)]

def generateUrls(i):
    global currentMood
    for url in log[str(i)]["urls"]:
        urlID = url["activity"]
        r = random.randint(0,len(variables["urls"][str(urlID)])-1)
        selectedUrl = variables["urls"][str(urlID)][r]
        url["url"] = selectedUrl

        currentMood = currentMood + people[currentPerson][urlID]

#################################################################################

########################
#     MAIN PROGRAM     #
########################

#Load variables & people
f = open('params/variables.json')
variables = json.load(f)

f = open('params/people.json')
people = json.load(f)

#Load templates
numTemplates = len([name for name in os.listdir('templates')])
for i in range(numTemplates):
    fname = "templates/template" + str(i+1) + ".json"
    f = open(fname)
    templates.append(json.load(f))

#Create folder for output if does not already exist
if not os.path.exists('data'):
    os.mkdir('data')

#Remove existing files in folder
logs = [name for name in os.listdir('data')]
for fname in logs:
    os.remove('data/' + fname)    

#Generate logs
for currentLog in range(numLogs):
    log = {}
    currentPerson = random.randint(0,len(people)-1) #Select person who log is for
    currentPerson = list(people.keys())[currentPerson] #Get key name for person index
    
    #Generate each timeslot for current log
    for i in range(24):
        currentMood = 0

        r = random.randint(0,numTemplates-1) #Template to choose this hour from
        log[str(i)] = copy.deepcopy(templates[r][str(i)]) #Add chunk

        #Generate data for chunk
        generateApps(i)
        generateCalls(i)
        generateTexts(i, "smsList")
        generateTexts(i, "smsOutList")
        generateLocations(i)
        generateUrls(i) 

        #Determine overall mood for chunk
        if currentMood > 0:
            log[str(i)]["mood"] = 1
        elif currentMood == 0:
            log[str(i)]["mood"] = 0
        else:
            log[str(i)]["mood"] = -1

    #Write log to file
    outputFile = "data/log_" + str(currentLog+1) + "_" + currentPerson + ".json"
    f = open(outputFile, 'w')
    json.dump(log, f, indent=4)
