-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi-3.0-batch-2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.batch-2.0))"
-bundles=com.ibm.ws.jbatch.cdi.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
