FROM mcr.microsoft.com/mssql/server:2019-CU10-ubuntu-16.04

COPY --chown=mssql:0 container-files/mssql.conf /var/opt/mssql/mssql.conf
COPY --chown=mssql:0 container-files/mssql.pem  /etc/ssl/certs/mssql.pem
COPY --chown=mssql:0 container-files/mssql.key  /etc/ssl/mssql.key


# Currently tagged in DockerHub as: kyleaure/sqlserver-ssl:2019-CU10-ubuntu-16.04
