-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.annotation-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=io.openliberty.jakarta.annotation.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.annotation:jakarta.annotation-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
