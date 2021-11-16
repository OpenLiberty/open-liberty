-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.graphql-2.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=io.openliberty.mpCompatible-5.0
-bundles=io.openliberty.org.eclipse.microprofile.graphql.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.graphql:microprofile-graphql-api:2.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
