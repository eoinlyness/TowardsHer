@echo off

echo Generating log files ...

py generate_log_data.py

echo Generating log files complete
echo.
echo Processing log data ...

py combine_logs.py

echo Processing log data complete
echo.
echo Performing clustering ...

py cluster.py

echo Clustering complete
echo.
echo Waiting to train model 1 (no privacy) ...
pause
echo Training model 1 (no privacy) ...

py collab.py

echo Training model 1 complete
echo.
echo Waiting to train model 2 (privacy) ...
pause
echo Training model 2 (privacy) ...

py collab_privacy.py

echo Training model 2 complete
echo.
echo Finished

pause