-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.api-6.1
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=io.openliberty.jakarta.servlet.6.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet:jakarta.servlet-api:6.1.0-M2"
kind=beta
edition=core
WLP-Activation-Type: parallel
