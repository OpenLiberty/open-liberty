## Docker images

This bucket uses several custom docker images:
 - A Kerberos (KDC) container  (e.g. 'kdc-jdbc-server:3.0.0.3')
 - A DB2 container              (e.g. 'db2-krb5:12.1.2.0')
 - An Oracle container          (e.g. 'oracle-krb5:23.26.2-full-faststart')
 - A Postgres container         (e.g. 'postgres-krb5:17.0.0.1')

The Dockerfile and supporting files for each image are stored in the sibling project:

    io.openliberty.org.testcontainers/resources/openliberty/testcontainers/<image-name>/<image-version>/Dockerfile

For example, the Postgres image source can be found at:

    io.openliberty.org.testcontainers/resources/openliberty/testcontainers/postgres-krb5/17.0.0.1/Dockerfile

Images are no longer pushed to a personal Docker Hub repository (e.g. 'kyleaure/...'). Instead,
the componenttest.containers.ImageBuilder class resolves images at test time using the following
priority order:

  1. Use the image if it is already cached on the local Docker host.
  2. Pull the image from the internal registry if one is available on the network.
  3. Build the image locally from the Dockerfile in io.openliberty.org.testcontainers.

This means no manual docker push step is required for CI builds.

### Updating an image

To modify an existing image:

  1. Update the Dockerfile (and any supporting files) under:
         io.openliberty.org.testcontainers/resources/openliberty/testcontainers/<image-name>/

  2. Create a new version directory with a higher version number. Do NOT overwrite existing version
     directories because in-flight remote builds may still reference the old version.

  3. Update the version string passed to ImageBuilder.build() in the corresponding container class,
     e.g. DB2KerberosContainer.java, OracleKerberosContainer.java, etc.

  4. Run a local build to confirm the image builds successfully before committing.


## Keytab files

Keytab files (.keytab) are generated dynamically at test start time by the KerberosContainer class
(see KerberosContainer.copyUserKeytab). They are extracted from the running KDC container and
written to the server security directory on demand, so there is no need to maintain pre-built
keytab files checked into source control.

If the KDC container image changes and you need to manually inspect or regenerate a keytab
outside of the test framework, you can do so as follows:

------------
# Run the bucket; the containers will be running once the bucket completes
docker ps
# Find the container ID of the KDC container
docker exec -it <KDC_CONTAINER> sh

# You are now in a shell inside the KDC container
kadmin.local

# Export the principal to a new keytab file
kadmin.local: ktadd -k /tmp/new_keytab.keytab <PRINCIPAL>
# May prompt for a password; the default password used by these containers is 'password'

# Copy the keytab file from the container to your local filesystem
docker cp <KDC_CONTAINER>:/tmp/new_keytab.keytab /some/path/on/your/system/
------------
