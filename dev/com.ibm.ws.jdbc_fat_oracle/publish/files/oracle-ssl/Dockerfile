FROM kyleaure/oracle-18.4.0-xe-prebuilt:2.0

COPY setup/ /tmp/setup/
COPY startup/ /opt/oracle/scripts/startup

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

EXPOSE 1522

# Currently tagged in DockerHub as: kyleaure/oracle-ssl-18.4.0-xe-prebuilt:2.0