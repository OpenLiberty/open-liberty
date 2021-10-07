-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing3.0-mpRestClient2.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpOpenTracing-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-2.0))"
-bundles=io.openliberty.microprofile.opentracing.3.0.internal.rest.client.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
