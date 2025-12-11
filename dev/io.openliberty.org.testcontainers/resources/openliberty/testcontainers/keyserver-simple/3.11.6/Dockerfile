# Base image used by external users is overwritten at runtime
# by a mirrored version from Artifactory for internal users.

ARG BASE_IMAGE="public.ecr.aws/docker/library/python:3.11.6"

FROM ${BASE_IMAGE}

WORKDIR /public_key

COPY public_key/validKey.asc validKey.asc

EXPOSE 8080

CMD ["python", "-m" , "http.server", "8080"]
