-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.optional.jaxb-2.3
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Java XML Bindings 2.3 for all Java versions
-features=\
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  com.ibm.websphere.javaee.activation.1.1; location:="dev/api/spec/,lib/"; apiJar=false, \
  com.ibm.websphere.javaee.jaxb.2.3; location:="dev/api/spec/,lib/"; apiJar=false, \
  com.ibm.ws.org.apache.geronimo.osgi.registry.1.1, \
  com.ibm.ws.jaxb.tools.2.3
kind=noship
edition=full
