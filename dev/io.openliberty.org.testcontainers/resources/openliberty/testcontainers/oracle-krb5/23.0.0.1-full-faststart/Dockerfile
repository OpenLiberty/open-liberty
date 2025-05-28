# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="ghcr.io/gvenzl/oracle-free:23-full-faststart"

FROM ${BASE_IMAGE}

# Update and install Kerberos (server head)
USER root
RUN microdnf update
RUN microdnf -y install krb5-workstation krb5-libs

# Copy setup and startup scripts
COPY setup/ /opt/oracle/scripts/setup
COPY startup/ /container-entrypoint-startdb.d/

# This variable tends to change with major updates.
# Update this variable when updating to Oracle 25
ENV ORACLE_ADMIN=/opt/oracle/product/23ai/dbhomeFree/network/admin

# Set environment variables
ENV ORACLE_PWD=password
ENV KRB5_REALM=EXAMPLE.COM
ENV KRB5_KDC=kerberos

# Initialize principles
RUN mkdir /etc/krb5
RUN printf 'add_entry -password -p FREE/oracle@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN printf 'add_entry -password -p oracle@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil

# Make scripts executable
RUN chmod 777 /opt/oracle/scripts/setup/1kerberos.sh
RUN chmod 777 /opt/oracle/scripts/setup/2oracle.sh
RUN chmod 777 /container-entrypoint-startdb.d/01_kerberos.sh
RUN chmod 777 /container-entrypoint-startdb.d/02_oracle.sql

# Run setup scripts
RUN /opt/oracle/scripts/setup/1kerberos.sh
RUN /opt/oracle/scripts/setup/2oracle.sh

# Remove setup scripts
RUN rm /opt/oracle/scripts/setup/1kerberos.sh
RUN rm /opt/oracle/scripts/setup/2oracle.sh 
 
USER oracle
