-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsConcurrent-1.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.0)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.concurrent-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.contextService-1.0
-bundles=com.ibm.ws.jaxrs.2.x.concurrent
kind=ga
edition=core
WLP-Activation-Type: parallel
