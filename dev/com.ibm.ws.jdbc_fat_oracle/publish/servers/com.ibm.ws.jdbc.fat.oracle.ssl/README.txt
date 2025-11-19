This server uses authentication credentials to create secure database connections. 

This includes the following files:

Oracle Wallet:
- cwallet.sso
- ewallet.p12

Java Key Store (JKS):
- client-keystore.jks
- client-truststore.jks

These files were generated when we built the database image:
* openliberty/testcontainers/oracle-ssl:23.9-full-faststart

These files are stored in the container under the following directories: 
- Oracle Wallet: /client/oracle/wallet
- Java Key Store (JKS): /client/oracle/store

These security related files are copied out of the container after it has started.
This process is automated as part of the test lifecycle.
