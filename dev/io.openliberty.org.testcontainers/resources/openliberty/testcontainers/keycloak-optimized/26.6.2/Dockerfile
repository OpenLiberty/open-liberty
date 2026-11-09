# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="quay.io/keycloak/keycloak:26.6.2"

FROM ${BASE_IMAGE}

# Build time options. The container must be started with `start --optimized`
# and must not change any of these at runtime, otherwise Keycloak will
# detect a configuration mismatch.
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_DB=dev-file

# Run the expensive Quarkus augmentation once at image build time instead of
# on every container start.
RUN /opt/keycloak/bin/kc.sh build

# Usage (the base image ENTRYPOINT is kc.sh):
#   start --optimized --http-enabled=true --hostname-strict=false \
#         --https-key-store-file=<file> --https-key-store-password=<password>
