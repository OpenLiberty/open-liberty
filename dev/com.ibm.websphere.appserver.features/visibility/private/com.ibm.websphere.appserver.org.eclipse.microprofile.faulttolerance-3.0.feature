-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-3.0
singleton=true
-features=com.ibm.websphere.appserver.javax.cdi-2.0, \
          io.openliberty.mpCompatible-4.0
-bundles=io.openliberty.org.eclipse.microprofile.faulttolerance.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:3.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
