# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/alpine:3.17"

FROM ${BASE_IMAGE}

RUN apk add openldap-clients openldap-back-mdb openldap krb5-server krb5 cyrus-sasl-gssapiv2 supervisor tini

# Copy files
COPY docker-entrypoint.sh /
COPY supervisord.conf /etc/

# ldap server configuration files
COPY config_files/full_example_com.ldif /etc/
COPY config_files/admin.keyfile /etc/
COPY config_files/kerberos.openldap.ldif /etc/

COPY config_files/ldap.conf /etc/openldap/
COPY config_files/slapd.ldif /etc/openldap/

ENTRYPOINT ["/sbin/tini", "--"]
RUN ["chmod", "+x", "/docker-entrypoint.sh"]
CMD ["/docker-entrypoint.sh"]
