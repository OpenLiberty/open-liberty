-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.faces-3.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.faces.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.myfaces.core:myfaces-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
