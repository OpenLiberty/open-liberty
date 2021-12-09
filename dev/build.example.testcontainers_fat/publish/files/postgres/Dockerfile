FROM postgres:11.2-alpine

COPY scripts/initDB.sql /docker-entrypoint-initdb.d/initDB.sql

RUN chown postgres /docker-entrypoint-initdb.d/initDB.sql && chmod 600 /docker-entrypoint-initdb.d/initDB.sql


# Currently tagged in DockerHub as: kyleaure/postgres-test-table:2.0
