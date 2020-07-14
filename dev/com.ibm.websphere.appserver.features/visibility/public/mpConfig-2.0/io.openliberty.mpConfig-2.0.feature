-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpConfig-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.config;  type="stable", \
  org.eclipse.microprofile.config.spi;  type="stable", \
  org.eclipse.microprofile.config.inject;  type="stable"
IBM-ShortName: mpConfig-2.0
Subsystem-Name: MicroProfile Config 2.0
-features=io.openliberty.org.eclipse.microprofile.config-2.0, \
 com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0, \
 com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:=1.3, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=com.ibm.ws.require.java8, \
 io.openliberty.microprofile.config.2.0.internal, \
 io.openliberty.microprofile.config.internal.common, \
 io.openliberty.microprofile.config.internal.serverxml, \
 com.ibm.ws.org.apache.commons.lang3, \
 com.ibm.ws.cdi.interfaces, \
 com.ibm.ws.org.jboss.logging
kind=noship
edition=full
WLP-Activation-Type: parallel
