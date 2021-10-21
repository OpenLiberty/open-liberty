-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.concurrency-3.0
visibility=private
singleton=true
#TODO update to eeCompatible-10.0 once other features are ready to use it
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.concurrency.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="io.openliberty.jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.0.0.20211013"
kind=noship
edition=full
WLP-Activation-Type: parallel
