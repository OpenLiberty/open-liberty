FROM postgres:11.2-alpine

COPY ssl-certs/server.crt /var/lib/postgresql/server.crt
COPY ssl-certs/server.key /var/lib/postgresql/server.key

RUN chown postgres /var/lib/postgresql/server.key && chmod 600 /var/lib/postgresql/server.key && \
    chown postgres /var/lib/postgresql/server.crt && chmod 600 /var/lib/postgresql/server.crt
    
# currently tagged as: aguibert/postgresql-ssl:1.0
