FROM mcr.microsoft.com/mssql/server:2019-CU2-ubuntu-16.04

COPY --chown=mssql:0 container-files/mssql.conf /var/opt/mssql/mssql.conf
COPY --chown=mssql:0 container-files/mssql.pem  /etc/ssl/certs/mssql.pem
COPY --chown=mssql:0 container-files/mssql.key  /etc/ssl/mssql.key
