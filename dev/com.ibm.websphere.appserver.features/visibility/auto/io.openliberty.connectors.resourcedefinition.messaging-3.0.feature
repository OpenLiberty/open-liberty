-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors.resourcedefinition.messaging-3.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.messaging.internal-3.0)(osgi.identity=io.openliberty.messaging.internal-3.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.injection-2.0))"
-bundles=com.ibm.ws.jca.resourcedefinition.jms.2.0.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=base
WLP-Activation-Type: parallel
