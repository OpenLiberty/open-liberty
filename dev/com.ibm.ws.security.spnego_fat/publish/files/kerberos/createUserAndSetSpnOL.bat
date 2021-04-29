set REMOVE_WIN_USERS=%1
set CREATE_WIN_USER=%2
set USER=%3
set PASSWORD=%4
set SERVICE_NAME=%5
set HOSTNAME=%6
set KDC=%7
set OP1=%8
set OP2=%9
shift
shift
shift
shift
shift
shift
shift
shift
shift

set OP3=%1
set OP4=%2
set OP5=%3
set OP6=%4
set OP7=%5
set OP8=%6
set OP9=%7
set OP10=%8
set OP11=%9
shift
shift
shift
shift
shift
shift
shift
shift
shift

set OP12=%1

echo %DATE% %TIME%
cscript %REMOVE_WIN_USERS% -user %USER% -host %KDC%
echo %DATE% %TIME%, Exit Code is %errorlevel%

cscript %CREATE_WIN_USER% -user %USER% -password %PASSWORD% -host %KDC% %OP1% %OP2% %OP3% %OP4% %OP5% %OP6% %OP7% %OP8% %OP9% %OP10% %OP11% %OP12%
echo %DATE% %TIME%, Exit Code is %errorlevel%

setspn -a %SERVICE_NAME%/%HOSTNAME% %USER%
echo %DATE% %TIME%, Exit Code is %errorlevel%
