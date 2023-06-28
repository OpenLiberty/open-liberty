-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-11.0
visibility=private
singleton=true
Subsystem-Version: 11.0.0
-bundles=com.ibm.ws.javaee.version, \
  io.openliberty.java17.internal
kind=noship
edition=full
WLP-Activation-Type: parallel
