-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.graphql-2.0
singleton=true
-features=io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1,7.0,7.1"
-bundles=io.openliberty.org.eclipse.microprofile.graphql.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.graphql:microprofile-graphql-api:2.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
