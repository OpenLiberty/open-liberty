-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.concurrency-2.0
visibility=private
singleton=true
-bundles=com.ibm.websphere.jakarta.concurrency.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:2.0.0-RC1"
kind=ga
edition=core
WLP-Activation-Type: parallel
