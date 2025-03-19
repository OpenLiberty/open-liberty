# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="ghcr.io/gvenzl/oracle-free:23-full-faststart"

FROM ${BASE_IMAGE}

COPY setup/ /tmp/setup/
COPY startup/ /container-entrypoint-startdb.d

# This variable tends to change with major updates.
# Update this variable when updating to Oracle 25
ENV ORACLE_ADMIN=/opt/oracle/product/23ai/dbhomeFree/network/admin

USER root

RUN chmod 777 /tmp/setup/enable_ssl.sh
RUN /tmp/setup/enable_ssl.sh
RUN rm -rf /tmp/setup

USER oracle

EXPOSE 1522

# Passing locally 02/19/2024
