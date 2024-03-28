-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.openapi-4.0
singleton=true
-features=io.openliberty.mpCompatible-7.0
#TODO check maven coords before GA
-bundles=io.openliberty.org.eclipse.microprofile.openapi.4.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.openapi:microprofile-openapi-api:4.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
