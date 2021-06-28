#!/bin/bash

echo "Generating log files ..."

python generate_log_data.py

echo "Generating log files complete"
echo
echo "Processing log data ..."

python combine_logs.py

echo "Processing log data complete"
echo
echo "Performing clustering ..."

python cluster.py

echo "Clustering complete"
echo
echo "Training model 1 (no privacy) ..."

python collab.py

echo "Training model 1 complete"
echo
echo "Training model 2 (privacy) ..."

python collab_privacy.py

echo "Training model 2 complete"
echo
echo "Finished"