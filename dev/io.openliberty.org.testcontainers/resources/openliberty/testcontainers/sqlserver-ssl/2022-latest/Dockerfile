# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="mcr.microsoft.com/mssql/server:2022-latest"

FROM ${BASE_IMAGE}

COPY container-files/mssql.conf /var/opt/mssql/mssql.conf
COPY startup/sqlserver_startup.sh /tmp/sqlserver_startup.sh

USER root
RUN chown mssql /var/opt/mssql/mssql.conf && \
    chmod 777 /tmp/sqlserver_startup.sh
RUN /tmp/sqlserver_startup.sh
USER mssql

# Passing locally 02/21/2024
