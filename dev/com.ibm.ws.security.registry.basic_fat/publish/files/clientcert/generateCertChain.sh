#!/bin/sh

#
# Generate a certificate chain for BasicUser3. The output will be the BasicUser3.jks and the
# CA's certificate, which can be imported to any required trust stores.
#

PASSWORD=security
VALIDITY=3650

ROOT_JKS=root.jks
ROOT_PEM=root.pem
ROOT_ALIAS=root

CA_JKS=ca.jks
CA_PEM=ca.pem
CA_ALIAS=ca

USER_JKS=BasicUser3.jks
USER_PEM=BasicUser3.pem
USER_ALIAS=basicuser3

rm -f ${ROOT_JKS} ${ROOT_PEM} ${CA_JKS} ${CA_PEM} ${USER_JKS} ${USER_PEM}

# Generate private keys
keytool -genkeypair -keystore ${ROOT_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${ROOT_ALIAS} -validity ${VALIDITY} -dname "cn=root"
keytool -genkeypair -keystore ${CA_JKS}   -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${CA_ALIAS}   -validity ${VALIDITY} -dname "cn=ca"
keytool -genkeypair -keystore ${USER_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${USER_ALIAS} -validity ${VALIDITY} -dname "cn=BasicUser3,o=ibm,c=us"

# Generate the root certificate
keytool -exportcert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc > ${ROOT_PEM}

# Generate the CA certificate (signed by root)
keytool -certreq    -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} \
  | keytool -gencert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc -validity ${VALIDITY} > ${CA_PEM}

# Import CA chain into the CA keystore
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} -file ${CA_PEM}

# Generate the user certificate (signed by ca, signed by root)
keytool -certreq    -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${USER_ALIAS} \
  | keytool -gencert -keystore ${CA_JKS} -storepass ${PASSWORD} -alias ${CA_ALIAS} -rfc -validity ${VALIDITY} > ${USER_PEM}

# Import cert chain into user keystore.
keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${CA_ALIAS}   -file ${CA_PEM}
keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${USER_ALIAS} -file ${USER_PEM}

rm -f ${ROOT_PEM}
rm -f ${ROOT_JKS}
#rm -f ${CA_PEM}
rm -f ${CA_JKS}
rm -f ${USER_PEM}

echo
echo "Install the root cert into any trust stores using the following command and then delete the 'root.pem' file:"
echo
echo "   keytool -keystore <TRUSTSTORE> -storepass <PASSWORD> -importcert -trustcacerts -noprompt -alias ${CA_ALIAS} -file ${CA_PEM}"
echo
