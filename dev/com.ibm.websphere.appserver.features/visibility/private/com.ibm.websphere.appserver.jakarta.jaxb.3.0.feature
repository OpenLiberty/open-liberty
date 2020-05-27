-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.jaxb-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta XML Bindings 3.0
-features=\
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  com.ibm.websphere.jakarta.activation.2.0;  location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.activation:jakarta.activation-api:2.0.0-RC3", \
  com.ibm.websphere.jakarta.jaxb.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.bind:jakarta.xml.bind-api:3.0.0-RC3", \
  com.ibm.ws.jaxb.3.0.tools
kind=noship
edition=core
WLP-Activation-Type: parallel