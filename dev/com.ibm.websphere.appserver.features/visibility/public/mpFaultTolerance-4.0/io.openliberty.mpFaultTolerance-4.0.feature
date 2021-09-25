-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-4.0
Subsystem-Name: MicroProfile Fault Tolerance 4.0
-features=io.openliberty.mpConfig-3.0, \
  io.openliberty.org.eclipse.microprofile.faulttolerance-4.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.cdi-3.0
-bundles=com.ibm.ws.microprofile.faulttolerance; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.0.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.jakarta; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.services; apiJar=false; location:="lib/",\
 io.openliberty.microprofile.faulttolerance.3.0.internal.cdi.jakarta; apiJar=false; location:="lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel
