-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsAppSecurity-2.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxrs.2.0.appSecurity
kind=ga
edition=core
