-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.batchSecurity-2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-4.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.batch-2.0))"
-features=com.ibm.websphere.appserver.servlet-5.0, \
 com.ibm.wsspi.appserver.webBundleSecurity-1.0
-bundles=com.ibm.ws.jbatch.security
IBM-Install-Policy: when-satisfied
kind=ga
edition=base
