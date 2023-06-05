-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.annotation-2.1
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0"
-bundles=io.openliberty.jakarta.annotation.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.annotation:jakarta.annotation-api:2.1.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
