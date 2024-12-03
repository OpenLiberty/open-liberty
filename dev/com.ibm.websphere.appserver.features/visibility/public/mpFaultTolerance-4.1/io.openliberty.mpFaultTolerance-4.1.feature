-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance-4.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-4.1
Subsystem-Name: MicroProfile Fault Tolerance 4.1
-features=io.openliberty.mpConfig-3.1, \
  io.openliberty.mpCompatible-7.0, \
  io.openliberty.org.eclipse.microprofile.faulttolerance-4.1, \
  io.openliberty.cdi-4.0; ibm.tolerates:="4.1"
-bundles=com.ibm.ws.microprofile.faulttolerance; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.0.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.services; apiJar=false; location:="lib/",\
 io.openliberty.microprofile.faulttolerance.3.0.internal.cdi.jakarta; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-7.0
