# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

# TODO cloudant is EOL consider removing or modifying the test that uses this image

ARG BASE_IMAGE="docker.io/ibmcom/cloudant-developer:2.0.1"

FROM ${BASE_IMAGE}

#Contains vulnerable log4j library for features not needed for testing
RUN rm -rf /opt/cloudant/search/
RUN rm -rf /opt/cloudant/etc/log4j.properties
