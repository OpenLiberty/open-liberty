-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient1.0-cdi1.2
visibility=private
singleton=true
IBM-API-Package: com.ibm.ws.microprofile.rest.client.cdi; type="internal"
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.0)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.1)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.2)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.3)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.4)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.cdi-1.2)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0)))"
-bundles=com.ibm.ws.microprofile.rest.client.cdi
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
