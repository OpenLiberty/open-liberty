# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="icr.io/db2_community/db2:12.1.1.0"

FROM ${BASE_IMAGE}

# Copy custom startup script that will run when server starts
COPY custom/ /var/custom/
RUN chmod 777 /var/custom/*

# Copy custom enablement script that will run when image is built
COPY setup/ /var/setup/
RUN chmod 777 /var/setup/* && /var/setup/enable-ssl.sh

# Make sure kdb and stash are readable
RUN chmod 755 /certs/*

# Force static environment variables
ENV LICENSE=accept
ENV DB2INSTANCE=db2inst1
ENV DB2INST1_PASSWORD=password
ENV DBNAME=testdb
ENV AUTOCONFIG=false
ENV ARCHIVE_LOGS=false

# Expose new port
EXPOSE 50001

# Passing locally 02/14/2024
