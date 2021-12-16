FROM kyleaure/oracle-18.4.0-expanded:1.0.full

COPY setup/ /tmp/setup/
COPY startup/ /container-entrypoint-startdb.d

USER root

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

USER oracle

EXPOSE 1522

# Currently tagged in DockerHub as: kyleaure/oracle-18.4.0-expanded:1.0.full.ssl
