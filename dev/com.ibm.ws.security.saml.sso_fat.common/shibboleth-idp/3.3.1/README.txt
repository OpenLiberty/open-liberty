This README is just is provided to give some hints for using Shibboleth as an embedded IDP for SAML SP testing
This in no way replaces any of the standard Shibboleth documentation (it exists to allow someone not familiar 
with Shibboleth, or someone that hasn't had to look at it in a long time to update the tooling used by our FAT
projects.


The Saml tests are setup to use a Shibboleth IDP.  The IDP is run as an app (idp.war) within a Liberty server.  
The war is currently located with in this project/dir (com.ibm.ws.security.saml.sso_fat/shibboleth-idp/war/idp.war).

If you need to update the level/version of Shibboleth.  Do the following:
1) Locate the version of Shibboleth that you want to use.  (Typically: https://shibboleth.net/downloads/identity-provider/latest/)
2) Download and unzip/untar/gunzip/... in a location on your machine (preferrebly somewhere other than your Liberty
	workspace - this instance will NOT be needed later)
3) cd into the directory where you unpacked
4) run "bin/install.<sh|bat>" (use sudo -E if needed (depends on your system setup))
	Answer prompts
	One question will be what will be the home - the config will refer to this via variable "idp.home" from now on.
		But, your response will also dictate where the install script will put the config/binaries/... on
		your system.  So, make sure this is a writable location and that you remember where you put it :)
	Once your done with the steps in this readme, you won't need this "installation" copy either 
5) Now, you need to put the "update" into the Liberty test framework
	A) For now, move com.ibm.ws.security.saml.sso_fat/shibboleth-idp to com.ibm.ws.security.saml.sso_fat/shibboleth-idp.keep (or someplace safe)
	B) Copy the Installation tree into Liberty
		cp <idp.home>/* com.ibm.ws.security.saml.sso_fat/shibboleth-idp/.
	C) The most important step now - Merge the config from the previous Shibboleth instance into 
		the version you just copied:
		1) the config should all be in com.ibm.ws.security.saml.sso_fat/shibboleth-idp/conf/*
		2) compare the files in the new conf directory against those in the conf directory of 
			com.ibm.ws.security.saml.sso_fat/shibboleth-idp.keep
		NOTE:
		  We can't predict the types of changes coming in the future.  You may be able to replace the new
		  config file with the copy that we've had in the test framework, or you may have to merge the contents.
		  Even if you merge the contents, you may end up with unset properties because of new function, ...
		  You'll just have to pay close attention to the behavior of the tests
	D) make sure that idp.home is NOT set in the config (without allowing for us to override it) - The FAT 
		framework will set idp.home to our runtime location.  The config can't point to a static location 
		that probably won't exist on a test system.
		

Misc:
The Shibboleth configuration has been updated to put the idp logs into the Liberty server log directory.  So, all of
	the logs should be available and in the same location as the standard Liberty logs.

Logging levers are controlled by settings within com.ibm.ws.security.saml.sso_fat/shibboleth-idp/conf/logback.xml.
	I've left some settings that have been helpful in dubugging setup issues - There is a block of properties
	that are commented out.





Generating key/cert files:
Started with one of our server trust stores (one that we had been using in TFIM)
I needed to generate a cert (crt) and key file for use within Shibboleth - used the following commands:
export the .crt:
	keytool -export -alias someCert -file someCert.der -keystore myTrustStore.jks
convert the cert to PEM:
	openssl x509 -inform der -in someCert.der -out someCert.pem
	mv someCert.pem someCert.crt
export the key:
	keytool -importkeystore -srckeystore myTrustStore.jks -destkeystore certKeyStore.p12 -deststoretype PKCS12
convert PKCS12 key to unencrypted PEM:
	openssl pkcs12 -in certKeyStore.p12  -nodes -nocerts -out someCertp12.key
	openssl rsa -in someCertp12.key -out someCert.key

