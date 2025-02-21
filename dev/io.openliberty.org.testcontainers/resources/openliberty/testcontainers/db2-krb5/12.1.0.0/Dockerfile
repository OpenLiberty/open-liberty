# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="icr.io/db2_community/db2:12.1.0.0"

FROM ${BASE_IMAGE}

# TODO 'krb5-auth-dialog' is no longer available from package managers
# that are configured on DB2 image. If needed download directly.
RUN yum -y install krb5-workstation krb5-libs

ADD docker-entrypoint.sh /
RUN chmod a+x /docker-entrypoint.sh

RUN mkdir /var/custom
COPY var/custom/createschema.sh /var/custom/createschema.sh
RUN chmod a+x /var/custom/createschema.sh

ENV LICENSE=accept
ENV DB2INSTANCE=db2inst1
ENV DB2INST1_PASSWORD=password
ENV DBNAME=testdb
ENV AUTOCONFIG=false
ENV ARCHIVE_LOGS=false

RUN mkdir /etc/krb5
RUN printf 'add_entry -password -p db2srvc@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN printf 'add_entry -password -p db2inst1@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil

ENTRYPOINT ["/docker-entrypoint.sh"]
