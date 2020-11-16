set USER=%1
set PASSWORD=%2
set SERVICE_NAME=%3
set HOSTNAME=%4
set KDC=%5

set OP1=%6
set OP2=%7
set OP3=%8
set OP4=%9
shift
shift
shift
shift
shift
shift
shift
shift
shift
set OP5=%1
set OP6=%2
set OP7=%3
set OP8=%4
set OP9=%5
set OP10=%6
set OP11=%7
set OP12=%8

cscript removeWinUsers.vbs -user %USER% -host %KDC%
cscript createWinUser.vbs -user %USER% -password %PASSWORD% -host %KDC% %OP1% %OP2% %OP3% %OP4% %OP5% %OP6% %OP7% %OP8% %OP9% %OP10% %OP11% %OP12%
setspn -a %SERVICE_NAME%/%HOSTNAME% %USER%
