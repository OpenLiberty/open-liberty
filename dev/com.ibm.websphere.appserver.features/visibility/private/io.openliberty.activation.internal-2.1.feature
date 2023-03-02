-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.activation.internal-2.1
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
IBM-API-Package: jakarta.activation; type="spec"
-features=io.openliberty.jakarta.activation-2.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  io.openliberty.org.eclipse.angus.activation
kind=beta
edition=core
WLP-Activation-Type: parallel
