-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlBinding.internal-4.0
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
-features=io.openliberty.jakarta.xmlBinding-4.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.xmlBinding.4.0.internal.tools
kind=ga
edition=core
WLP-Activation-Type: parallel
