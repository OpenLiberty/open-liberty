-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.jwt-2.1
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-6.0; ibm.tolerates:="6.1,7.0,7.1"
-bundles=io.openliberty.org.eclipse.microprofile.jwt.2.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:2.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
