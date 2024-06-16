-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-11.0
visibility=private
singleton=true
Subsystem-Version: 11.0.0
-bundles=com.ibm.ws.javaee.version, \
  io.openliberty.java17.internal
 kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-11.0