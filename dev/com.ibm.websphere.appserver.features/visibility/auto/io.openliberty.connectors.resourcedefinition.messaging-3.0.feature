-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors.resourcedefinition.messaging-3.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.messaging-3.0.internal))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.injection-2.0))"
-features=io.openliberty.jakarta.connectors-2.0
-bundles=com.ibm.ws.jca.resourcedefinition.jms.2.0.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=base
WLP-Activation-Type: parallel
