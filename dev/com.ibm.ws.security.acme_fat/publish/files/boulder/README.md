# Update Boulder Container Certificates

## Generate the Updated Certificates.

Update and run the `generateCertificates.sh` script to generate new certificates.

`bash$ ./generateCertificates.sh`

## Pull The Latest Docker Image

Pull the latest docker image from docker hub.

`bash$ docker pull ryanesch/acme-boulder`

## Check Installed Images

Verify that the latest Docker image was installed.

```
bash$ docker images
REPOSITORY                        TAG       IMAGE ID       CREATED         SIZE
mariadb                           10.3      f14696e18c23   5 days ago      384MB
testcontainers/ryuk               0.3.1     ee7515743e6f   4 months ago    12MB
letsencrypt/pebble-challtestsrv   latest    5be704a8068c   6 months ago    15.6MB
letsencrypt/pebble                latest    7213864a87a0   6 months ago    16.9MB
testcontainers/sshd               1.0.0     b665446a05c2   11 months ago   7.8MB
ryanesch/acme-boulder             1.1       f008f51703e1   13 months ago   2.1GB
ryanesch/acme-boulder             latest    f008f51703e1   13 months ago   2.1GB
```

## Deploy the container

Deploy the container by issuing the following command where `f008f51703e1` is the image ID
of the image you want to update, output by the `docker images` command issued above. The
container will now be running in the background.

```
bash$ docker run -dt f008f51703e1

bash$ docker ps
CONTAINER ID   IMAGE          COMMAND   CREATED         STATUS         PORTS     NAMES
9997c5e150f6   f008f51703e1   "bash"    3 seconds ago   Up 2 seconds             stupefied_diffie
```

## Modify the container

We need to add the generated certificates to the running Boulder container. Issue the following
commands to copy the new certificates to the running container. The `9997c5e150f6` is the ID of
the running container, output by the `docker ps` command issued above.

```
bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-ca.der 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/

bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-ca.pem 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/

bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-ca2.der 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/

bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-ca2.pem 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/

bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-root.der 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/

bash$ docker cp com.ibm.ws.security.acme_fat/publish/files/boulder/test-root.pem 9997c5e150f6:/go/src/github.com/letsencrypt/boulder/test/
```

## Commit changes to image

Commit the changes to a new image. The `9997c5e150f6` is the ID of the running 
container, output by the `docker ps` command issued above. The `[new_image_name]` should probably
be the same image name, but with an incremented tag - `ryanesch/ryanesch/acme-boulder:1.2`
would be an example. The tag (1.2) should be an increment over the last version so it does not conflict.

```
bash$ docker commit 9997c5e150f6 ryanesch/acme-boulder:1.2
```

Verify the new image exists.

```
bash$ docker images
REPOSITORY                        TAG       IMAGE ID       CREATED         SIZE
mariadb                           10.3      f14696e18c23   5 days ago      384MB
testcontainers/ryuk               0.3.1     ee7515743e6f   4 months ago    12MB
letsencrypt/pebble-challtestsrv   latest    5be704a8068c   6 months ago    15.6MB
letsencrypt/pebble                latest    7213864a87a0   6 months ago    16.9MB
testcontainers/sshd               1.0.0     b665446a05c2   11 months ago   7.8MB
ryanesch/acme-boulder             1.1       f008f51703e1   13 months ago   2.1GB
ryanesch/acme-boulder             1.2       43669de57745   9 seconds ago   2.1GB
ryanesch/acme-boulder             latest    f008f51703e1   13 months ago   2.1GB
```

Stop the running container. The `9997c5e150f6` is the ID of the running 
container, output by the `docker ps` command issued above.

```
bash$ docker stop 9997c5e150f6
```

## Test with Local Image

Update the `BoulderContainer.DOCKER_IMAGE` to point to your new image and run the ACME 
FATs in FULL mode.

```
bash$ ./gradlew com.ibm.ws.security.acme_fat:buildandrun -Dfat.test.mode=full
```

## Push the image to Docker Hub

We need to point the latest tag to the new tag (1.2 in this scenario). So delete
the latest tag for the image and point tag 1.2 as latest. Then login with your
Docker Hub credentials and push to the repo.

```
bash$ docker rmi ryanesch/acme-boulder:latest
bash$ docker image tag ryanesch/acme-boulder:1.2 ryanesch/acme-boulder:latest
bash$ docker login
bash$ docker push ryanesch/acme-boulder:1.2 ryanesch/acme-boulder:latest
```

## Test with Docker Hub Images

You can test again by deleting the images from your local repository and pulling
it from Docker Hub.

```
bash$ docker rmi ryanesch/acme-boulder:1.2 ryanesch/acme-boulder:latest
bash$ ./gradlew com.ibm.ws.security.acme_fat:buildandrun -Dfat.test.mode=full
```

## Commit changes to Git

These includes the changes to BoulderContainer, as well as the newly generated certificates.
