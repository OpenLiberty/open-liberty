-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.faulttolerance.exceptions;  type="stable", \
                 org.eclipse.microprofile.faulttolerance;  type="stable"
IBM-ShortName: mpFaultTolerance-2.0
Subsystem-Name: MicroProfile Fault Tolerance 2.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-1.1, \
 com.ibm.websphere.appserver.concurrent-1.0, \
 com.ibm.websphere.appserver.mpConfig-1.1; ibm.tolerates:="1.2, 1.3"
-bundles=com.ibm.ws.net.jodah.failsafe.1.0.4; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.2.0; apiJar=false; location:="lib/", \
 com.ibm.ws.microprofile.faulttolerance.spi; apiJar=false; location:="lib/"
kind=noship
edition=full
