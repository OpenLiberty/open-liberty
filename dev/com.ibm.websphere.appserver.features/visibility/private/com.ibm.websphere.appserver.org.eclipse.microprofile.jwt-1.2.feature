-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.jwt-1.2
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-4.0
-bundles=io.openliberty.org.eclipse.microprofile.jwt.1.2; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.2"
kind=beta
edition=core
WLP-Activation-Type: parallel
