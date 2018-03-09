-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient1.0-ssl1.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.0)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ssl-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.jaxrsClient-2.0; ibm.tolerates:=2.1
-bundles=com.ibm.ws.microprofile.rest.client.ssl
kind=ga
edition=core
