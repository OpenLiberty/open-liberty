-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.acme-1.0
visibility=public
singleton=true
IBM-ShortName: acme-1.0
Subsystem-Version: 1.0
Subsystem-Name: acme 1.0
-features=\
  com.ibm.websphere.appserver.acme-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0
-bundles=\
  com.ibm.ws.security.acme; start-phase:=APPLICATION_EARLY, \
  com.ibm.ws.org.jose4j.0.5.1; version="[1.0.0,1.0.200)", \
  com.ibm.ws.com.google.gson.2.2.4; version="[1.0.0,1.0.200)", \
  com.ibm.ws.org.slf4j.api.1.7.7; version="[1.0.0,1.0.200)", \
  com.ibm.ws.org.slf4j.jdk14.1.7.7; version="[1.0.0,1.0.200)", \
  com.ibm.ws.org.bcprov.jdk15on.1.60, \
  com.ibm.ws.org.bcpkix.jdk15on.1.60 
kind=noship
edition=core