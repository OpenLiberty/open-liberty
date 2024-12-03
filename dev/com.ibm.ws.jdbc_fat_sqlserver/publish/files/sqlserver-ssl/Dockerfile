# TODO update to mcr.microsoft.com/mssql/server:2019-CU28-ubuntu-20.04

FROM mcr.microsoft.com/mssql/server:2019-CU18-ubuntu-20.04

COPY --chown=mssql:0 container-files/mssql.conf /var/opt/mssql/mssql.conf
COPY --chown=mssql:0 container-files/mssql.pem  /etc/ssl/certs/mssql.pem
COPY --chown=mssql:0 container-files/mssql.key  /etc/ssl/mssql.key


# Currently tagged in DockerHub as: kyleaure/sqlserver-ssl:2019-CU18-ubuntu-20.04
