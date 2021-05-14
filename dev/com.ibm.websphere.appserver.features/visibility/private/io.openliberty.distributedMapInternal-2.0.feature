-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.distributedMapInternal-2.0
visibility=private
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 2.0.0
-features=io.openliberty.servlet.api-5.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.dynacache.internal
kind=beta
edition=core
WLP-Activation-Type: parallel
