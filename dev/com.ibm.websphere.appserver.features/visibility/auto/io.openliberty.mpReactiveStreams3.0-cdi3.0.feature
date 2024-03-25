-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpReactiveStreams3.0-cdi3.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpReactiveStreams-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=io.openliberty.cdi-4.0)))"
-bundles=io.openliberty.microprofile.reactive.streams.operators30.cdi
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
