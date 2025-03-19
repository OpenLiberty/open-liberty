# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/couchdb:3"

FROM ${BASE_IMAGE}

COPY couchdb-config/testcontainers_config.ini /opt/couchdb/etc/local.d/

COPY startup/couch-startup.sh /tmp/couch-startup.sh
RUN chmod 777 /tmp/couch-startup.sh && /tmp/couch-startup.sh

# Passing locally 02/21/2024
