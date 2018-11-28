-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpConfig-1.4
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.config;  type="stable", \
  org.eclipse.microprofile.config.spi;  type="stable", \
  org.eclipse.microprofile.config.inject;  type="stable"
IBM-ShortName: mpConfig-1.4
Subsystem-Name: MicroProfile Config 1.4
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.4, \
 com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0, \
 com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:=1.3, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0
-bundles=com.ibm.ws.require.java8, \
 com.ibm.ws.microprofile.config; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.config.1.2; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.config.1.3; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.config.1.4; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.config.1.4.services; apiJar=false; location:="lib/", \
 com.ibm.ws.cdi.interfaces; apiJar=false; location:="lib/", \
 com.ibm.ws.org.apache.commons.lang3; apiJar=false; location:="lib/"
kind=noship
edition=full
