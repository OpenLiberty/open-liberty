-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.messaging-3.0
singleton=true
-bundles=io.openliberty.jakarta.messaging.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.jms:jakarta.jms-api:3.0.0"
-features=com.ibm.websphere.appserver.eeCompatible-9.0
kind=beta
edition=base
WLP-Activation-Type: parallel
