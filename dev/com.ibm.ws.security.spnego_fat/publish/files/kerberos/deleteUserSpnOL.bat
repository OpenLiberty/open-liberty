set USER=%1
set SERVICE_NAME=%2
set HOSTNAME=%3

echo %DATE% %TIME%
setspn -D %SERVICE_NAME%/%HOSTNAME% %USER%
echo %DATE% %TIME%, Exit Code is %errorlevel%
