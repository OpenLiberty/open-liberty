-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-2.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-2.1
Subsystem-Name: MicroProfile Fault Tolerance 2.1
-features=com.ibm.websphere.appserver.mpConfig-1.4; ibm.tolerates:="1.1,1.2,1.3", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-2.1, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.concurrent-1.0
-bundles=com.ibm.ws.microprofile.faulttolerance; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.0.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.services; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
