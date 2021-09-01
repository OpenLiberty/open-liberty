-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eeCompatible-10.0
visibility=private
#TODO once EE 10 features are available, switch to singleton to prohibit combining with other EE level features
singleton=false
Subsystem-Version: 10.0.0
-bundles=com.ibm.ws.javaee.version
kind=noship
edition=full
WLP-Activation-Type: parallel
