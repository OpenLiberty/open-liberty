-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.messaging-3.1
singleton=true
-bundles=io.openliberty.jakarta.messaging.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.jms:jakarta.jms-api:3.1.0"
-features=com.ibm.websphere.appserver.eeCompatible-10.0
kind=ga
edition=base
WLP-Activation-Type: parallel
