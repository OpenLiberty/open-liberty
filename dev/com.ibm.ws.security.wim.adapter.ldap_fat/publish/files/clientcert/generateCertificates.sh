#!/bin/sh

###############################################################################
# Copyright (c) 2021 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
###############################################################################

###############################################################################
#
# Generate the certificates for LDAP clientcert testing.
#
###############################################################################

PASSWORD=security
VALIDITY=3650

ROOT_ALIAS=chain_root
ROOT_JKS=${ROOT_ALIAS}.jks
ROOT_PEM=${ROOT_ALIAS}.pem

CA_ALIAS=chain_ca
CA_JKS=${CA_ALIAS}.jks
CA_PEM=${CA_ALIAS}.pem

DUMMY_KEYSTORE_JKS=DummyServerKeyFile.jks
DUMMY_TRUSTSTORE_JKS=DummyServerTrustFile.jks
DUMMY_SERVER_ALIAS=dummyserver

USER1_JKS=LDAPUser1.jks
USER1_ALIAS=ldapuser1

USER2_JKS=LDAPUser2.jks
USER2_ALIAS=ldapuser4

USER3_JKS=LDAPUser3.jks
USER3_PEM=LDAPUser3.pem
USER3_ALIAS=ldapuser3

USER5_JKS=LDAPUser5.jks
USER5_ALIAS=ldapuser5

USER1_XTRA_JKS=LDAPUser1ExtraOID.jks
USER1_XTRA_ALIAS=ldapuser1email

USER1_INVALID_JKS=LDAPUser1Invalid.jks
USER1_INVALID_ALIAS=ldapuser1invalid

###############################################################################

rm -f ${ROOT_JKS} ${ROOT_PEM} ${CA_JKS} ${CA_PEM} ${USER3_JKS} ${USER3_PEM}

# Generate private keys
keytool -genkeypair -keyalg RSA -keystore ${ROOT_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${ROOT_ALIAS} -validity ${VALIDITY} -dname "cn=${ROOT_ALIAS}" -ext BC=ca:true -ext KU=keyCertSign
keytool -genkeypair -keyalg RSA -keystore ${CA_JKS}   -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${CA_ALIAS}   -validity ${VALIDITY} -dname "cn=${CA_ALIAS}"
keytool -genkeypair -keyalg RSA -keystore ${USER3_JKS} -storepass ${PASSWORD} -keypass ${PASSWORD} -alias ${USER3_ALIAS} -validity ${VALIDITY} -dname "cn=LDAPUser3,o=IBM,c=US"

# Generate the root certificate
keytool -exportcert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc > ${ROOT_PEM}

# Generate the CA certificate (signed by root)
keytool -certreq    -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} \
  | keytool -gencert -keystore ${ROOT_JKS} -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -rfc -validity ${VALIDITY} -ext BC=ca:true -ext KU=keyCertSign > ${CA_PEM}

# Import CA chain into the CA keystore
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${CA_JKS}   -storepass ${PASSWORD} -alias ${CA_ALIAS} -file ${CA_PEM}

# Generate the user certificate (signed by ca, signed by root)
keytool -certreq    -keystore ${USER3_JKS}  -storepass ${PASSWORD} -alias ${USER3_ALIAS} \
  | keytool -gencert -keystore ${CA_JKS} -storepass ${PASSWORD} -alias ${CA_ALIAS} -rfc -validity ${VALIDITY} -ext EKU=serverAuth,clientAuth > ${USER3_PEM}

# Import cert chain into user keystore.
#keytool -importcert -keystore ${USER3_JKS}  -storepass ${PASSWORD} -alias ${ROOT_ALIAS} -trustcacerts -noprompt -file ${ROOT_PEM}
keytool -importcert -keystore ${USER3_JKS}  -storepass ${PASSWORD} -alias ${CA_ALIAS}   -noprompt -file ${CA_PEM}
keytool -importcert -keystore ${USER3_JKS}  -storepass ${PASSWORD} -alias ${USER3_ALIAS} -noprompt -file ${USER3_PEM}

rm -f ${ROOT_JKS}
rm -f ${CA_JKS}

#echo
#echo "Install the CA cert into any trust stores using the following commands:"
#echo
#echo "   keytool -keystore <TRUSTSTORE> -storepass <PASSWORD> -delete -alias ${CA_ALIAS}"
#echo "   keytool -keystore <TRUSTSTORE> -storepass <PASSWORD> -importcert -trustcacerts -noprompt -alias ${CA_ALIAS} -file ${CA_PEM}"
#echo


#
# dummyserver
#
rm -f ${DUMMY_KEYSTORE_JKS}
keytool -genkeypair -keyalg RSA -alias ${DUMMY_SERVER_ALIAS} -dname CN=DummyServer,O=IBM,C=US -validity ${VALIDITY} -keystore ${DUMMY_KEYSTORE_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${DUMMY_SERVER_ALIAS} -file cert.crt -keystore ${DUMMY_KEYSTORE_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${DUMMY_SERVER_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${DUMMY_SERVER_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

#
# ldapuser1
#
rm -f ${USER1_JKS}
keytool -genkeypair -keyalg RSA -alias ${USER1_ALIAS} -dname CN=LDAPUser1,O=IBM,C=US -validity ${VALIDITY} -keystore ${USER1_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${USER1_ALIAS} -file cert.crt -keystore ${USER1_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${USER1_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${USER1_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

#
# ldapuser2
#
rm -f ${USER2_JKS}
keytool -genkeypair -keyalg RSA -alias ${USER2_ALIAS} -dname CN=LDAPUser2 -validity ${VALIDITY} -keystore ${USER2_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${USER2_ALIAS} -file cert.crt -keystore ${USER2_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${USER2_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${USER2_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

#
# ldapuser3 (really the CA cert)
#
keytool -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD} -importcert -trustcacerts -noprompt -alias ${CA_ALIAS} -file ${CA_PEM}

#
# ldapuser5
#
rm -f ${USER5_JKS}
keytool -genkeypair -keyalg RSA -alias ${USER5_ALIAS} -dname CN=LDAPUser5,O=IBM,C=US -validity ${VALIDITY} -keystore ${USER5_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${USER5_ALIAS} -file cert.crt -keystore ${USER5_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${USER5_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${USER5_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

#
# ldapuser1extraoid
#
rm -f ${USER1_XTRA_JKS}
keytool -genkeypair -keyalg RSA -alias ${USER1_XTRA_ALIAS} -dname CN=LDAPUser1,C=US,O=IBM,EMAILADDRESS=badWolf@badwolf.com -validity ${VALIDITY} -keystore ${USER1_XTRA_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${USER1_XTRA_ALIAS} -file cert.crt -keystore ${USER1_XTRA_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${USER1_XTRA_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${USER1_XTRA_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

#
# ldapuser1invalid
#
rm -f ${USER1_INVALID_JKS}
keytool -genkeypair -keyalg RSA -alias ${USER1_INVALID_ALIAS} -dname CN=LDAPUser1,O=INVALID,C=US -validity ${VALIDITY} -keystore ${USER1_INVALID_JKS} -storepass ${PASSWORD}
keytool -export -noprompt -rfc -alias ${USER1_INVALID_ALIAS} -file cert.crt -keystore ${USER1_INVALID_JKS} -storepass ${PASSWORD}
keytool -delete -alias ${USER1_INVALID_ALIAS} -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}
keytool -import -noprompt -alias ${USER1_INVALID_ALIAS} -file cert.crt -keystore ${DUMMY_TRUSTSTORE_JKS} -storepass ${PASSWORD}

rm cert.crt
