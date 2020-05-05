-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.activation-2.0
singleton=true
-bundles=\
  io.openliberty.jakarta.activation.2.0; location:="dev/api/spec/,lib/";mavenCoordinates="jakarta.activation:jakarta.activation-api:2.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
