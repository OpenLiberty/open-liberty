# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

# TODO - consider switching to alpine based postgres image to increase build time performance
ARG BASE_IMAGE="public.ecr.aws/docker/library/postgres:17"

FROM ${BASE_IMAGE}

# Install kerberos - set config options so apt does not prompt for user input
RUN set -ex; \
    echo krb5-config krb5-config/default_realm string LOCALDOMAIN | debconf-set-selections; \
    echo krb5-config krb5-config/kerberos_servers string | debconf-set-selections; \
    echo krb5-config krb5-config/add_servers_realm string LOCALDOMAIN | debconf-set-selections; \
    apt-get update; \
    printf '\n' | apt-get install -y --no-install-recommends \
        krb5-user \
        libpam-krb5 \
        libpam-ccreds;

# Default environment
ENV KRB5_REALM=EXAMPLE.COM
ENV KRB5_KDC=kerberos

# Copy config
COPY files/krb5.conf /etc/krb5.conf

# Prime keytab location
RUN chmod -R 777 /etc/

COPY startup/db_setup.sh /docker-entrypoint-initdb.d/db_setup.sh
RUN chmod 777 /docker-entrypoint-initdb.d/db_setup.sh
