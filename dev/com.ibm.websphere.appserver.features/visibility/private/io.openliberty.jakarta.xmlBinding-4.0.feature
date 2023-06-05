-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.xmlBinding-4.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta XML Bindings 4.0
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.activation-2.1
-bundles=\
  io.openliberty.jakarta.xmlBinding.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.bind:jakarta.xml.bind-api:4.0.0", \
  io.openliberty.org.glassfish.hk2.osgi-resource-locator
kind=ga
edition=core
WLP-Activation-Type: parallel
