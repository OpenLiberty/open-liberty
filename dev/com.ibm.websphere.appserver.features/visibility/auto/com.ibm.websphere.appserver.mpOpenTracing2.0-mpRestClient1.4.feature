-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenTracing2.0-mpRestClient1.4
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpOpenTracing-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.4))"
-bundles=com.ibm.ws.microprofile.opentracing.rest.client.2.0
IBM-Install-Policy: when-satisfied
kind=noship
edition=core
