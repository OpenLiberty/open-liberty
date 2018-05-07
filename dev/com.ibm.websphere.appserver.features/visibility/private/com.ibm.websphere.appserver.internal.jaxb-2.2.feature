-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jaxb-2.2
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Internal Java XML Bindings 2.2
-features=\
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  com.ibm.websphere.javaee.jaxb.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.bind:jaxb-api:2.2.12", \
  com.ibm.ws.org.apache.geronimo.osgi.registry.1.1, \
  com.ibm.ws.jaxb.tools.2.2.10
kind=ga
edition=core
