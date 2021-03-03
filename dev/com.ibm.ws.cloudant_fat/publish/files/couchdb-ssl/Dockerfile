FROM couchdb:2.3

COPY couchdb-config/testcontainers_config.ini /opt/couchdb/etc/local.d/
COPY ssl-certs/couchdb.pem /etc/couchdb/cert/
COPY ssl-certs/privkey.pem /etc/couchdb/cert/

RUN chmod 644 /etc/couchdb/cert/*

# Currently tagged and pushed as aguibert/couchdb-ssl:1.0