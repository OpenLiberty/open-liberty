-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.optional.corba-1.5
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: OMG CORBA APIs and RMI-IIOP API
-bundles=\
  io.openliberty.org.apache.bcel; require-java:="9",\
  com.ibm.ws.org.apache.commons.lang3; require-java:="9", \
  io.openliberty.yoko.spec.corba; require-java:="9",\
  io.openliberty.yoko.osgi; require-java:="9",\
  io.openliberty.yoko.rmi.impl; require-java:="9",\
  io.openliberty.yoko.core; require-java:="9",\
  io.openliberty.yoko.util; require-java:="9",\
  io.openliberty.yoko.rmi.spec; require-java:="9"
kind=ga
edition=core
