-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsSSLSecurity-2.1
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.appSecurity-2.0
-bundles=com.ibm.ws.jaxrs.2.0.security
kind=beta
edition=core
