This server uses authentication credentials to create secure database connections. 

This includes the following files:

Oracle Wallet:
- cwallet.sso
- ewallet.p12

Java Key Store (JKS):
- client-keystore.jks
- client-truststore.jks

These files were generated when we built the database image:
* kyleaure/oracle-21.3.0-faststart:1.0.full.ssl

This process has been automated so that future changes to this project will be easier.
To build a new database image and re-generate these files go to:
* /publish/files/oracle-ssl/

And run the script:
* release.sh

This will build the docker image, extract the security files, and push the image to dockerhub.

It should be noted that this docker image SHOULD NOT be used for any production workload.
This is only a test database image. The trusted certificates are kept in plain text within the docker image. 