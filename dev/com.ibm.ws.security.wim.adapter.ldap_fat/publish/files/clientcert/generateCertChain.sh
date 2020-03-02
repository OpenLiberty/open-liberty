#!/bin/sh

#
# Generate a certificate chain for LDAPUser3. The output will be the LDAPUser3.jks and the
# CA's certificate, which can be imported to any required trust stores.
#

PASSWORD=security
VALIDITY=3650

ROOT_ALIAS=chain_root
ROOT_JKS=${ROOT_ALIAS}.jks
ROOT_PEM=${ROOT_ALIAS}.pem

CA_ALIAS=chain_ca
CA_JKS=${CA_ALIAS}.jks
CA_PEM=${CA_ALIAS}.pem

USER_JKS=LDAPUser3.jks
USER_PEM=LDAPUser3.pem
USER_ALIAS=ldapuser3

rm -f ${ROOT_JKS} ${ROOT_PEM} ${CA_JKS} ${CA_PEM} ${USER_JKS} ${USER_PEM}

# Generate private keys
keytool -genkeypair -keyalg RSA -keystore ${ROOT_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${ROOT_ALIAS} -validity ${VALIDITY} -dname "cn=${ROOT_ALIAS}" -ext BC=ca:true -ext KU=keyCertSign
keytool -genkeypair -keyalg RSA -keystore ${CA_JKS}   -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${CA_ALIAS}   -validity ${VALIDITY} -dname "cn=${CA_ALIAS}"
keytool -genkeypair -keyalg RSA -keystore ${USER_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${USER_ALIAS} -validity ${VALIDITY} -dname "cn=LDAPUser3,o=IBM,c=US"

# Generate the root certificate
keytool -exportcert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc > ${ROOT_PEM}

# Generate the CA certificate (signed by root)
keytool -certreq    -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} \
  | keytool -gencert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc -validity ${VALIDITY} -ext BC=ca:true -ext KU=keyCertSign > ${CA_PEM}

# Import CA chain into the CA keystore
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} -file ${CA_PEM}

# Generate the user certificate (signed by ca, signed by root)
keytool -certreq    -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${USER_ALIAS} \
  | keytool -gencert -keystore ${CA_JKS} -storepass ${PASSWORD} -alias ${CA_ALIAS} -rfc -validity ${VALIDITY} -ext EKU=serverAuth,clientAuth > ${USER_PEM}

# Import cert chain into user keystore.
#keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${CA_ALIAS}   -noprompt -file ${CA_PEM}
keytool -importcert -keystore ${USER_JKS}  -storepass ${PASSWORD} -alias ${USER_ALIAS} -noprompt -file ${USER_PEM}

#rm -f ${ROOT_PEM}
rm -f ${ROOT_JKS}
#rm -f ${CA_PEM}
rm -f ${CA_JKS}
#rm -f ${USER_PEM}

echo
echo "Install the CA cert into any trust stores using the following commands:"
echo
echo "   keytool -keystore <TRUSTSTORE> -storepass <PASSWORD> -delete -alias ${CA_ALIAS}"
echo "   keytool -keystore <TRUSTSTORE> -storepass <PASSWORD> -importcert -trustcacerts -noprompt -alias ${CA_ALIAS} -file ${CA_PEM}"
echo
