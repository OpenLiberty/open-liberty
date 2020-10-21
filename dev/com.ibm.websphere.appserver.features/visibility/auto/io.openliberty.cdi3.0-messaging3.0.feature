-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-messaging3.0
visibility=private
IBM-App-ForceRestart: install,uninstall
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))",\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jakarta.messaging-3.0))"
-bundles=com.ibm.ws.messaging.jms.2.0.cdi.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
