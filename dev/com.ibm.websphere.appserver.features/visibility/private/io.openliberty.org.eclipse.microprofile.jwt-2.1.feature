-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.jwt-2.1
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-6.0
-bundles=io.openliberty.org.eclipse.microprofile.jwt.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:2.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
