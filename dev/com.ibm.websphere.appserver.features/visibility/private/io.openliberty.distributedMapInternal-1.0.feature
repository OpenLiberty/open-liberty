-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.distributedMapInternal-1.0
visibility=private
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.0.0
-features=\
  com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1,4.0",\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0"
-bundles=\
  com.ibm.ws.dynacache
kind=ga
edition=core
WLP-Activation-Type: parallel
