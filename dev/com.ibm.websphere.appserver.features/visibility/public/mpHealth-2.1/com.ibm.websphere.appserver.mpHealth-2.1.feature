-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpHealth-2.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.health;  type="stable", \
  org.eclipse.microprofile.health.spi;  type="stable"
IBM-ShortName: mpHealth-2.1
Subsystem-Name: MicroProfile Health 2.1
-features=com.ibm.websphere.appserver.json-1.0, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.org.eclipse.microprofile.health-2.1, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:="2.0"
-bundles=\
 com.ibm.websphere.jsonsupport, \
 com.ibm.ws.microprofile.health.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.org.joda.time.1.6.2
kind=ga
edition=core
WLP-Activation-Type: parallel
