# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/postgres:17-alpine"

FROM ${BASE_IMAGE}

COPY scripts/initDB.sql /docker-entrypoint-initdb.d/initDB.sql

RUN chown postgres /docker-entrypoint-initdb.d/initDB.sql && chmod 600 /docker-entrypoint-initdb.d/initDB.sql
