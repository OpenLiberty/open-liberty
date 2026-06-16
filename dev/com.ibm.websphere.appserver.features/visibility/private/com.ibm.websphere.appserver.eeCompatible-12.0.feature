-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-12.0
visibility=private
singleton=true
Subsystem-Version: 12.0.0
-bundles=io.openliberty.jakartaee.platform.v12, \
 com.ibm.ws.javaee.version
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-12.0
