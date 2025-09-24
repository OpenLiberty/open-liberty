# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="icr.io/db2_community/db2:12.1.2.0"

FROM ${BASE_IMAGE}

# This variable tends to change with major updates.
# Update this variable when updating to DB2 v13
ENV DB2_ADMIN=/opt/ibm/db2/V12.1

# Copy custom startup script that will run when server starts
COPY custom/ /var/custom/
RUN chmod 777 /var/custom/*

# Copy entrypoint
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod 777 /docker-entrypoint.sh

# TODO 'krb5-auth-dialog' is no longer available from package managers
# that are configured on DB2 image. If needed download directly.
RUN yum -y install krb5-workstation krb5-libs

# Create key table for service and user accounts
# As of DB2 12 the default users have a principle of <username>/<external-hostname>@EXAMPLE.COM
RUN mkdir /etc/krb5
RUN printf 'add_entry -password -p db2srvc@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN printf 'add_entry -password -p db2inst1/db2@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil

RUN chmod 777 /etc/krb5.keytab
RUN chmod 777 ${DB2_ADMIN}/adm/db2start

ENTRYPOINT ["/docker-entrypoint.sh"]
