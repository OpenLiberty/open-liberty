-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-1.0
singleton=true
-features=com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0
-bundles=com.ibm.websphere.org.eclipse.microprofile.faulttolerance.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
