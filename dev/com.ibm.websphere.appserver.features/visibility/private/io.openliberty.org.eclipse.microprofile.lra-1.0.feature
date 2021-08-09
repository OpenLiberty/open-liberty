-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.lra-1.0
singleton=true
-bundles=io.openliberty.org.eclipse.microprofile.lra.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.lra:microprofile-lra-api:1.0-M1"
-features=io.openliberty.mpCompatible-4.0; ibm.tolerates:="0.0", \
  com.ibm.websphere.appserver.javax.jaxrs-2.1
kind=beta
edition=core
WLP-Activation-Type: parallel
