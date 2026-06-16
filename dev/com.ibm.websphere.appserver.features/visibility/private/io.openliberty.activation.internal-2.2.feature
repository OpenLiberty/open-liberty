-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.activation.internal-2.2
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
IBM-API-Package: jakarta.activation; type="spec"
-features=io.openliberty.jakarta.activation-2.2, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  io.openliberty.org.eclipse.angus.activation
kind=noship
edition=full
WLP-Activation-Type: parallel
