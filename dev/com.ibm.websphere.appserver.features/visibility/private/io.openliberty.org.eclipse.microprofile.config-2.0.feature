-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.config-2.0
singleton=true
-features=com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0
-bundles=io.openliberty.org.eclipse.microprofile.config.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:2.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
