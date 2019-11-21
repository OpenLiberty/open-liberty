-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsSecurity-2.3
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.3))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxwsweb-2.3))"
-bundles=com.ibm.ws.jaxws.2.3.security
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
