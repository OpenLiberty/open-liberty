FROM ibmcom/db2:11.5.0.0a

RUN yum update -y
RUN yum -y install krb5-workstation krb5-libs krb5-auth-dialog

ADD docker-entrypoint.sh /
RUN chmod a+x /docker-entrypoint.sh

RUN mkdir /var/custom
COPY var/custom/createschema.sh /var/custom/createschema.sh
RUN chmod a+x /var/custom/createschema.sh

ENV DB2INSTANCE=db2inst1
ENV DB2INST1_PASSWORD=password
ENV DBNAME=testdb
ENV AUTOCONFIG=false
ENV ARCHIVE_LOGS=false

RUN mkdir /etc/krb5
RUN printf 'add_entry -password -p db2srvc@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN printf 'add_entry -password -p db2inst1@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil

ENTRYPOINT ["/docker-entrypoint.sh"]