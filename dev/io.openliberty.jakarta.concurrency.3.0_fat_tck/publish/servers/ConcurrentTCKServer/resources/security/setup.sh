#!/bin/bash

#Script to use to generate the arquillian and liberty keystores

exp=360000
dn="CN=localhost"

arqAlias="arquillian"
arqPass="arquillianPassword"
arqStore="arquillian.p12"
arqCert="arquillian.crt"

libAlias="liberty"
libPass="libertyPassword"
libStore="liberty.p12"
libCert="liberty.crt"

function echoEnter() {
	echo "---> $1"
}

function echoExit() {
	echo "<--- $1"
}

#Generate keys
echoEnter "Generate Keys"
keytool -genkey -alias $arqAlias -dname $dn \
				-validity $exp -keyalg RSA -keysize 2048 -storetype pkcs12 \
				-keypass $arqPass  -storepass $arqPass \
				-keystore $arqStore
keytool -genkey -alias $libAlias -dname $dn \
				-validity $exp -keyalg RSA -keysize 2048 -storetype pkcs12 \
				-keypass $libPass -storepass $libPass \
				-keystore $libStore
echoExit "Generate Keys"


#Export certificates
echoEnter "Export certs"
keytool -export -alias $arqAlias  -storepass $arqPass -keystore $arqStore -file $arqCert
keytool -export -alias $libAlias  -storepass $libPass -keystore $libStore -file $libCert
echoExit "Export certs"

#Trade certificates
echoEnter "Trade trusted certs"
keytool -import -trustcacerts -noprompt -storepass $arqPass -keystore $arqStore -alias $libAlias -file $libCert
keytool -import -trustcacerts -noprompt -storepass $libPass -keystore $libStore -alias $arqAlias -file $arqCert
echoExit "Trade trusted certs"

#Remove cert files
echoEnter "Remove cert files"
rm $arqCert
rm $libCert
echoExit "Remove cert files"

#Output finalized data
echoEnter "Output Arquillian Keystore"
keytool -list -storepass $arqPass -keystore $arqStore
echoExit "Output Arquillian Keystore"

echoEnter "Output Liberty Keystore"
keytool -list -storepass $libPass -keystore $libStore
echoExit "Output Liberty Keystore"