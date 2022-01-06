FROM couchdb:3.2.0

COPY couchdb-config/testcontainers_config.ini /opt/couchdb/etc/local.d/
COPY ssl-certs/couchdb.pem /etc/couchdb/cert/
COPY ssl-certs/privkey.pem /etc/couchdb/cert/

RUN chmod 644 /etc/couchdb/cert/*

# Currently tagged in DockerHub as: kyleaure/couchdb-ssl:1.0
