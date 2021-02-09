set USER=%1
set PASSWORD=%2
set SERVICE_NAME=%3
set HOSTNAME=%4
set KEYTAB=%5
set REALM=%6
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
set OP12=%9

cscript removeWinUsers.vbs -user %USER% -host %KDC%
cscript createWinUser.vbs -user %USER% -password %PASSWORD% -host %KDC% %OP1% %OP2% %OP3% %OP4% %OP5% %OP6% %OP7% %OP8% %OP9% %OP10% %OP11% %OP12%
setspn -a %SERVICE_NAME%/%HOSTNAME% %USER%
ktpass -out %KEYTAB% -in localhost_HTTP_krb5.keytab -princ %SERVICE_NAME%/%HOSTNAME%@%REALM% -mapUser %USER%@%REALM% -mapOp set -pass %PASSWORD% -crypto RC4-HMAC-NT -kvno 0 -ptype KRB5_NT_PRINCIPAL