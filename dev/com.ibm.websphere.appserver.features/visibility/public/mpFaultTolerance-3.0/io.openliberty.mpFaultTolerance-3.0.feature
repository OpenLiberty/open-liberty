-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-3.0
Subsystem-Name: MicroProfile Fault Tolerance 3.0
-features=io.openliberty.org.eclipse.microprofile.faulttolerance-3.0, \
 com.ibm.websphere.appserver.cdi-2.0, \
 com.ibm.websphere.appserver.concurrent-1.0, \
 io.openliberty.mpConfig-2.0
-bundles=com.ibm.ws.microprofile.faulttolerance; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.0.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi; apiJar=false; location:="lib/",\
 com.ibm.ws.microprofile.faulttolerance.2.1.cdi.services; apiJar=false; location:="lib/",\
 io.openliberty.microprofile.faulttolerance.3.0.internal.cdi; apiJar=false; location:="lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel
