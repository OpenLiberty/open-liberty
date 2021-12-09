-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.xmlBinding-4.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta XML Bindings 4.0
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.activation-2.1
-bundles=\
  io.openliberty.jakarta.xmlBinding.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.bind:jakarta.xml.bind-api:3.0.1", \
  io.openliberty.org.glassfish.hk2.osgi-resource-locator
kind=noship
edition=full
WLP-Activation-Type: parallel
