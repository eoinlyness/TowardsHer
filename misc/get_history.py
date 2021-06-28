#
# Author: Eoin Lyness
# Get Chrome web history for previous day from the machine
#
import os
from sys import platform
import sqlite3
import datetime
import time
import pandas as pd

#Convert Windows timestamp to unix format
def convert_timestamp(timestamp):
    if platform == "win32":
        len_timestamp = len(str(timestamp))
        if len_timestamp < 18:
            x = 18 - len_timestamp
            timestamp = timestamp * (10 ** x)        
        return datetime.datetime(1601, 1, 1) + datetime.timedelta(seconds=timestamp/10000000)
    else:
        return datetime.datetime.fromtimestamp(timestamp)

today = datetime.date.today()
tomorrow = today + datetime.timedelta(days=1)

timestamp_today = int(time.mktime(today.timetuple()))
timestamp_tomorrow = int(time.mktime(tomorrow.timetuple()))

loc = ""
if platform == "win32":
    W_EPOCH = datetime.date(1601, 1, 1)
    timestamp_today = int((today - W_EPOCH).total_seconds() * (10**6))
    timestamp_tomorrow = int((tomorrow - W_EPOCH).total_seconds() * (10**6))

    loc = os.path.expanduser("~/AppData/Local/Google/Chrome/User Data/Default/History")
elif platform == "darwin":
    loc = os.path.expanduser("~/Library/Application Support/Google/Chrome/Default/History")
elif platform == "linux":
    loc = os.path.expanduser("~/.config/google-chrome/Default/History")

timestamp_today = str(timestamp_today)
timestamp_tomorrow = str(timestamp_tomorrow)

con = sqlite3.connect(loc)
query = ("SELECT title, urls.url, last_visit_time, visits.visit_duration, visit_count FROM urls INNER JOIN visits ON " +  
        "visits.url = urls.id WHERE (visit_duration > 0) AND (last_visit_time BETWEEN cast(" + timestamp_today + " as text) AND cast(" + timestamp_tomorrow + " as text)) ORDER BY visit_time DESC")

data = pd.read_sql_query(query, con)
data['last_visit_time'] = data['last_visit_time'].apply(convert_timestamp)
data['last_visit_time'] = data['last_visit_time'].dt.round('1s')

data['visit_duration'] = data['visit_duration'] / (10**6)
    
print(data.head())