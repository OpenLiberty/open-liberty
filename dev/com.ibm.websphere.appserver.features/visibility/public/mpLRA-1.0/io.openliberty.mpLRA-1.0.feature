-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpLRA-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpLRA-1.0
Subsystem-Name: MicroProfile Long Running Actions 1.0
IBM-API-Package: \
  org.eclipse.microprofile.lra.annotation; type="stable", \
  org.eclipse.microprofile.lra.annotation.ws.rs; type="stable";
-features=io.openliberty.org.eclipse.microprofile.lra-1.0, \
          com.ibm.websphere.appserver.jaxrs-2.1
-bundles=io.openliberty.microprofile.lra.1.0.internal, \
         io.openliberty.org.jboss.narayana.rts, \
         com.ibm.ws.org.jboss.logging
kind=noship
edition=full
