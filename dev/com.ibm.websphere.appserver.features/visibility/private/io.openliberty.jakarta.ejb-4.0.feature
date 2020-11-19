-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.ejb-4.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.ejb.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ejb:jakarta.ejb-api:4.0.0-RC2"
kind=beta
edition=core
WLP-Activation-Type: parallel
