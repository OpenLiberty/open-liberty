-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpConfig-3.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.config;  type="stable", \
  org.eclipse.microprofile.config.spi;  type="stable", \
  org.eclipse.microprofile.config.inject;  type="stable", \
  io.smallrye.config; type="internal"
IBM-ShortName: mpConfig-3.1
Subsystem-Name: MicroProfile Config 3.1
-features=com.ibm.websphere.appserver.appmanager-1.0, \
  io.openliberty.jakarta.annotation-2.1, \
  io.openliberty.org.eclipse.microprofile.config-3.1, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  io.openliberty.mpCompatible-6.1
-bundles=io.openliberty.io.smallrye.config3, \
 io.openliberty.io.smallrye.common2, \
 io.openliberty.microprofile.config.internal.common, \
 io.openliberty.microprofile.config.internal.serverxml, \
 com.ibm.ws.org.apache.commons.lang3, \
 com.ibm.ws.cdi.interfaces.jakarta, \
 io.openliberty.org.jboss.logging35
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
