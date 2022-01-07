-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-3.0
visibility=private
singleton=true
#TODO remove toleration of eeCompatible-9.0 once other EE 10 features are usable in beta form
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="9.0"
-bundles=io.openliberty.jakarta.concurrency.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="io.openliberty.jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.0.0.20211206"
kind=noship
edition=full
WLP-Activation-Type: parallel
