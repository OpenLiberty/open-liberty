# TODO update to 17.0-alpine

FROM postgres:14.1-alpine

COPY scripts/initDB.sql /docker-entrypoint-initdb.d/initDB.sql

RUN chown postgres /docker-entrypoint-initdb.d/initDB.sql && chmod 600 /docker-entrypoint-initdb.d/initDB.sql


# Currently tagged in DockerHub as: kyleaure/postgres-test-table:3.0
