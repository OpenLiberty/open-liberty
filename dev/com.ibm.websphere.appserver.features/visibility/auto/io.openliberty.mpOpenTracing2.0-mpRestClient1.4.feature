-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing2.0-mpRestClient1.4
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpOpenTracing-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpRestClient-1.4))"
-bundles=io.openliberty.microprofile.opentracing.2.0.internal.rest.client
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
