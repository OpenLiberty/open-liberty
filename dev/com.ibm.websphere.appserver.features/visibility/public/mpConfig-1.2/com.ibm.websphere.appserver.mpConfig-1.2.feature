-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpConfig-1.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.config;  type="stable", \
  org.eclipse.microprofile.config.spi;  type="stable", \
  org.eclipse.microprofile.config.inject;  type="stable"
IBM-ShortName: mpConfig-1.2
Subsystem-Name: MicroProfile Config 1.2
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:="1.3", \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.2
-bundles=io.openliberty.microprofile.config.internal.common, \
 com.ibm.ws.microprofile.config.1.1, \
 com.ibm.ws.microprofile.config.1.2, \
 com.ibm.ws.microprofile.config.1.2.services, \
 com.ibm.ws.org.apache.commons.lang3
kind=ga
edition=core

WLP-Activation-Type: parallel
