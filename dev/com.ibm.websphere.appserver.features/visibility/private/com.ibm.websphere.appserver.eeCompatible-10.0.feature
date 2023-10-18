-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-10.0
visibility=private
singleton=true
Subsystem-Version: 10.0.0
-bundles=com.ibm.ws.javaee.version, \
  io.openliberty.java11.internal
-features=io.openliberty.microProfile.internal-6.0
kind=ga
edition=core
WLP-Activation-Type: parallel
