-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi2.0-jsf2.3
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jsf-2.3))"
-bundles=com.ibm.ws.cdi.2.0.jsf
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
