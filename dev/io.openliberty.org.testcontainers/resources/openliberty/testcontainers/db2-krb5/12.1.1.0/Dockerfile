# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="icr.io/db2_community/db2:12.1.1.0"

FROM ${BASE_IMAGE}

ENV LICENSE=accept
ENV DB2INSTANCE=db2inst1
ENV DB2INST1_PASSWORD=password
ENV DBNAME=testdb
ENV AUTOCONFIG=false
ENV ARCHIVE_LOGS=false

# Copy custom startup script that will run when server starts
COPY custom/ /var/custom/
RUN chmod 777 /var/custom/*

# Copy entrypoint
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod 777 /docker-entrypoint.sh

# TODO 'krb5-auth-dialog' is no longer available from package managers
# that are configured on DB2 image. If needed download directly.
RUN yum -y install krb5-workstation krb5-libs

ENTRYPOINT ["/docker-entrypoint.sh"]

//TODO This currently does not work need to do more debugging to figure out
//why the database cannot be restarted after enabling kerberos
