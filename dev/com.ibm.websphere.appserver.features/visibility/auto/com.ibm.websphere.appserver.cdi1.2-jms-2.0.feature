-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi1.2-jms-2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.internal.jms-2.0))"
-bundles=com.ibm.ws.messaging.jms.2.0.cdi
IBM-Install-Policy: when-satisfied
kind=ga
edition=base
