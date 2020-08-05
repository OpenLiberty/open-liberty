-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging.defaultresource-2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.messagingClient-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.messagingServer-2.0))"
-bundles=com.ibm.ws.messaging.jms.defaultresource
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
