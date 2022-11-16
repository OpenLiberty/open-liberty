-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-3.0
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.jakarta.concurrency.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.0.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
