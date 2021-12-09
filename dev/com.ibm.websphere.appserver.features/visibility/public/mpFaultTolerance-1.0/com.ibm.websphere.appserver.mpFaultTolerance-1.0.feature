-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-1.0
Subsystem-Name: MicroProfile Fault Tolerance 1.0
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.mpConfig-1.1; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.concurrent-1.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-1.0
-bundles=com.ibm.ws.net.jodah.failsafe.1.0.4; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.1.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
