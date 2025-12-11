# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/alpine:3.17"

FROM ${BASE_IMAGE}

# Install Kerberos
RUN apk add --no-cache krb5-server krb5 supervisor tini

# Copy configuration and entrypoint
COPY supervisord.conf /etc/supervisord.conf
COPY docker-entrypoint.sh /

# Expose ports
EXPOSE 749 464 88

# Entrypoint and command
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/docker-entrypoint.sh"]
