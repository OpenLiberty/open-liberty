FROM kyleaure/oracle-18.4.0-xe-prebuilt

RUN yum update -y
RUN yum -y install vim

COPY setup/ /opt/oracle/scripts/setup/

ENV ORACLE_PWD=password

RUN chmod 777 /opt/oracle/scripts/setup/enable_ssl.sh && \
    /opt/oracle/scripts/setup/enable_ssl.sh && \
    rm /opt/oracle/scripts/setup/enable_ssl.sh 
 