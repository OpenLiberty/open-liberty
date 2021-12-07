-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.annotation-2.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0"
-bundles=io.openliberty.jakarta.annotation.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.annotation:jakarta.annotation-api:2.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
