# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/postgres:17"

FROM ${BASE_IMAGE}

COPY startup/db2_startup.sh /tmp/db2_startup.sh
RUN chmod 777 /tmp/postgre_startup.sh && /tmp/postgre_startup.sh

# Passing locally 02/21/2024
