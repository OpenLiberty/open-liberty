-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.connectors-2.1
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.connectors.2.1; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.resource:jakarta.resource-api:2.1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
