-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.health;  type="stable", \
  org.eclipse.microprofile.health.spi;  type="stable"
IBM-ShortName: mpHealth-3.0
Subsystem-Name: MicroProfile Health 3.0
-features=io.openliberty.org.eclipse.microprofile.health-3.0, \
 com.ibm.websphere.appserver.cdi-2.0,\
 com.ibm.websphere.appserver.classloaderContext-1.0, \
 com.ibm.websphere.appserver.contextService-1.0, \
 com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.json-1.0, \
 com.ibm.websphere.appserver.servlet-4.0,\
 io.openliberty.mpConfig-2.0,\
 com.ibm.wsspi.appserver.webBundle-1.0 
-bundles=com.ibm.websphere.jsonsupport, \
 com.ibm.ws.classloader.context, \
 io.openliberty.microprofile.health.3.0.internal; apiJar=false; location:="lib/", \
 com.ibm.ws.org.joda.time.1.6.2
kind=beta
edition=core
WLP-Activation-Type: parallel
