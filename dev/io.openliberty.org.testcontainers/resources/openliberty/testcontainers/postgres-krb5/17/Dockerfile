# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/postgres:17"

FROM ${BASE_IMAGE}

RUN set -ex; \
    echo krb5-config krb5-config/default_realm string LOCALDOMAIN | debconf-set-selections; \
    echo krb5-config krb5-config/kerberos_servers string | debconf-set-selections; \
    echo krb5-config krb5-config/add_servers_realm string LOCALDOMAIN | debconf-set-selections; \
    apt-get update; \
    printf '\n' | apt-get install -y --no-install-recommends \
        krb5-user \
        libpam-krb5 \
        libpam-ccreds \
        vim \
        inetutils-ping;

ENV KRB5_REALM=EXAMPLE.COM
ENV KRB5_KDC=kerberos

ADD files/krb5.conf /etc/krb5.conf

# Initialize static keytab entries. We also have 1 dynamic keytab entry for postgres/<hostname>@EXAMPLE.com
# which needs to be initialized at runtime via db_setup.sh
RUN printf 'add_entry -password -p postgres/postgresql@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN printf 'add_entry -password -p pguser@EXAMPLE.COM -k 1 -e aes256-cts\npassword\nwkt /etc/krb5.keytab' | ktutil
RUN chmod 777 /etc/krb5.keytab

ADD startup/db_setup.sh /docker-entrypoint-initdb.d/db_setup.sh
RUN chmod 777 /docker-entrypoint-initdb.d/db_setup.sh
