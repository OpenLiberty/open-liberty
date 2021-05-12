-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-2.0
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.concurrency.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:2.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
