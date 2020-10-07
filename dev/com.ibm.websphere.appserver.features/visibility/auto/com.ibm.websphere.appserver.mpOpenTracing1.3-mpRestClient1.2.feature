-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenTracing1.3-mpRestClient1.2
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpOpenTracing-1.3))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.2)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.3)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.4)))"
-bundles=com.ibm.ws.microprofile.opentracing.rest.client.1.3
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
