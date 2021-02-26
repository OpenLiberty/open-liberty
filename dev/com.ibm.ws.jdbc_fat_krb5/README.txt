## Docker images

This bucket uses several custom docker images:
 - A kerberos container (e.g. 'aguibert/krb5-server:1.1')
 - A DB2 container (e.g. '')
 - An Oracle container (e.g. 'aguibert/krb5-oracle:1.0')
 
The files necessary to build each container can be found under the 'publish/files/' folder. For example, to rebuild the Oracle container you can do:

------------
cd publish/files/oracle
docker build . -t <DOCKER_USERNAME>/krb5-oracle:<NEW_VERSION>

# Ensure you push the new image up to docker hub so remote builds can use it
# WARNING: Always use a new (higher) version number if you rebuild. Do NOT overwrite existing tags because
#          in-flight remote builds would immediately pick up the new container without running a personal build first
docker push <DOCKER_USERNAME>/krb5-oracle:<NEW_VERSION>
------------


## Keytab files

There are a few .keytab files in this bucket, which are permanently checked in. However, if the KDC or DB container ever changes, you may need
to rebuild the keytab files and check in the updated version.  For example:

------------
# run the bucket, containers should be running after the bucket completes
docker ps
# find the ID of the KDC container
docker exec -it <KDC_CONTAINER> sh

# you are now in a shell inside the KDC container
kadmin.local

# extract the principal to a new keytab
kadmin.local: ktadd -k /tmp/new_keytab.keytab ORACLEUSR
# may prompt for password, which is always just "password"

# extract the keytab file from the docker container to your local file system
docker cp <KDC_CONTAINER>:/tmp/new_keytab.keytab /some/path/on/your/system/

# then, replace the old keytab file with the new keytab you just extracted to your local file system
------------
