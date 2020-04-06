-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrscdi-2.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.jndi-1.0
-bundles=com.ibm.ws.jaxrs.2.0.cdi
kind=ga
edition=core
WLP-Activation-Type: parallel
