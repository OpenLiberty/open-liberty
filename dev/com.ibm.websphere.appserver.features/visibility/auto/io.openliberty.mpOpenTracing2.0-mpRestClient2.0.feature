-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing2.0-mpRestClient2.0
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpOpenTracing-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpRestClient-2.0))"
-bundles=io.openliberty.microprofile.opentracing.2.0.internal.rest.client
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
