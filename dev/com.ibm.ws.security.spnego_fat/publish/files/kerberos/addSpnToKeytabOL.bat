set USER=%1
set PASSWORD=%2
set SERVICE_NAME=%3
set HOSTNAME=%4
set KEYTAB=%5
set REALM=%6

echo %DATE% %TIME%
setspn -A %SERVICE_NAME%/%HOSTNAME% %USER%
echo %DATE% %TIME%, Exit Code is %errorlevel%

ktpass -in %KEYTAB% -out %KEYTAB% -princ %SERVICE_NAME%/%HOSTNAME%@%REALM% -mapUser %USER%@%REALM% -mapOp add -pass %PASSWORD% -crypto RC4-HMAC-NT -kvno 0
echo %DATE% %TIME%, Exit Code is %errorlevel%
