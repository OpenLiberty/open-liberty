-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.annotation-2.0
singleton=true
IBM-Process-Types: server, \
 client
-bundles=io.openliberty.jakarta.annotation.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.annotation:jakarta.annotation-api:2.0.0-RC1"
kind=beta
edition=core
WLP-Activation-Type: parallel
