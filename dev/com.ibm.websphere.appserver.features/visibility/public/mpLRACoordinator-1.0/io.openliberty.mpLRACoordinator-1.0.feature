-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpLRACoordinator-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpLRACoordinator-1.0
Subsystem-Name: MicroProfile Long Running Actions Coordinator 1.0
-features=com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  io.openliberty.mpCompatible-4.0; ibm.tolerates:="0.0", \
  io.openliberty.org.eclipse.microprofile.lra-1.0
IBM-API-Package: \
  org.eclipse.microprofile.lra.annotation; type="stable", \
  org.eclipse.microprofile.lra.annotation.ws.rs; type="stable";
-bundles= \
    io.openliberty.microprofile.lra.coordinator.1.0.internal
-files= \
    lib/mpLRACoordinator_5.10.6.jar
kind=beta
edition=core
WLP-Activation-Type: parallel

