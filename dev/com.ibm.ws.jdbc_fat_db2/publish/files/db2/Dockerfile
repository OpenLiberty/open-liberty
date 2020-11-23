FROM ibmcom/db2:11.5.0.0a

RUN mkdir /var/custom
COPY certs/ /certs/
COPY custom/ /var/custom/

RUN chmod 777 /var/custom/*

ENV LICENSE=accept
ENV DB2INSTANCE=db2inst1
ENV DB2INST1_PASSWORD=password
ENV DBNAME=testdb
ENV AUTOCONFIG=false
ENV ARCHIVE_LOGS=false
