# TODO update to use gvenzl/oracle-free to test Oracle 23c
FROM gvenzl/oracle-xe:21.3.0-full-faststart

COPY setup/ /tmp/setup/
COPY startup/ /container-entrypoint-startdb.d

# Oracle admin moved from /opt/oracle/oradata/dbconfig/XE/ to /opt/oracle/homes/OraDBHome21cXE/network/admin in 21c
ENV ORACLE_ADMIN=/opt/oracle/homes/OraDBHome21cXE/network/admin

USER root

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

USER oracle

EXPOSE 1522

# Currently tagged in DockerHub as: kyleaure/oracle-21.3.0-faststart:1.0.full.ssl
