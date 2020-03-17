-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectors-2.0
visibility=public
singleton=true
IBM-ShortName: connectors-2.0
IBM-API-Package: jakarta.resource; type="spec"
Subsystem-Name: Jakarta EE Connectors 2.0
-features=\
 com.ibm.websphere.appserver.jakarta.connectors-2.0
-bundles=\
 com.ibm.websphere.javaee.connector.1.7; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.resource:javax.resource-api:1.7";
kind=noship
edition=core
WLP-Activation-Type: parallel
