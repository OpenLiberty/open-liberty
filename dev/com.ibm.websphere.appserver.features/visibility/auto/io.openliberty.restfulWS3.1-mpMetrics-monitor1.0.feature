-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS3.1-mpMetrics-monitor1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpMetrics-5.0)(osgi.identity=io.openliberty.mpMetrics-5.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.restfulWS-3.1))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.restfulWS.mpMetrics.filter
kind=ga
edition=core