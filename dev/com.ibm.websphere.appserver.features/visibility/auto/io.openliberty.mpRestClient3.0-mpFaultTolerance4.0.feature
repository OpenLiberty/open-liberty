-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpRestClient3.0-mpFaultTolerance4.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpRestClient-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpFaultTolerance-4.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.microprofile.rest.client.3.0.internal.ft
kind=ga
edition=core
WLP-Activation-Type: parallel