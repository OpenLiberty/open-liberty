-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.jaxb-3.0.optional
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
  io.openliberty.jakarta.activation.2.0; location:="dev/api/spec/,lib/"; apiJar=false, \
  io.openliberty.jakarta.jaxb.3.0; location:="dev/api/spec/,lib/"; apiJar=false, \
  io.openliberty.jaxb.3.0.internal.tools
kind=noship
edition=full
WLP-Activation-Type: parallel