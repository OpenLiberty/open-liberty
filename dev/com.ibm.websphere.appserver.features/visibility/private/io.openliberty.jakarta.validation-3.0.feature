-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.validation-3.0
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.validation.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.validation:jakarta.validation-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
