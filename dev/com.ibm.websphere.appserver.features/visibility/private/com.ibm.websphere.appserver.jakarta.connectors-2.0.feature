-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.connectors-2.0
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.jakarta.transaction-2.0
-bundles=com.ibm.websphere.jakarta.connectors.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.resource:jakarta.resource-api:2.0.0-RC1"
kind=ga
edition=core
WLP-Activation-Type: parallel
