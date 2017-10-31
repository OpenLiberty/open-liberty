-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi2.0-batch1.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.batch-1.0))"
-bundles=com.ibm.ws.jbatch.cdi
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
