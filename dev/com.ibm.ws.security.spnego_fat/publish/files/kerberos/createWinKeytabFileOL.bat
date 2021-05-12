set REMOVE_WIN_USERS=%1
set CREATE_WIN_USER=%2
set USER=%3
set PASSWORD=%4
set SERVICE_NAME=%5
set HOSTNAME=%6
set KEYTAB=%7
set REALM=%8
set KDC=%9
shift
shift
shift
shift
shift
shift
shift
shift
shift

set OP1=%1
set OP2=%2
set OP3=%3
set OP4=%4
set OP5=%5
set OP6=%6
set OP7=%7
set OP8=%8
set OP9=%9
shift
shift
shift
shift
shift
shift
shift
shift
shift

set OP10=%1
set OP11=%2
set OP12=%3

echo %DATE% %TIME%
cscript %REMOVE_WIN_USERS% -user %USER% -host %KDC%
echo %DATE% %TIME%, Exit Code is %errorlevel%

cscript %CREATE_WIN_USER% -user %USER% -password %PASSWORD% -host %KDC% %OP1% %OP2% %OP3% %OP4% %OP5% %OP6% %OP7% %OP8% %OP9% %OP10% %OP11% %OP12%
echo %DATE% %TIME%, Exit Code is %errorlevel%

setspn -a %SERVICE_NAME%/%HOSTNAME% %USER%
echo %DATE% %TIME%, Exit Code is %errorlevel%

ktpass -out %KEYTAB% -in localhost_HTTP_krb5.keytab -princ %SERVICE_NAME%/%HOSTNAME%@%REALM% -mapUser %USER%@%REALM% -mapOp set -pass %PASSWORD% -crypto RC4-HMAC-NT -kvno 0 -ptype KRB5_NT_PRINCIPAL +Answer
echo %DATE% %TIME%, Exit Code is %errorlevel%
