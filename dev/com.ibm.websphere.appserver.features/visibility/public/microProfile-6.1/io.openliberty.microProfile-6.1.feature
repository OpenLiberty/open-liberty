-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.microProfile-6.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-6.1
Subsystem-Name: MicroProfile 6.1
-features=\
  io.openliberty.cdi-4.0, \
  io.openliberty.jsonb-3.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.restfulWS-3.1, \
  io.openliberty.restfulWSClient-3.1, \
  io.openliberty.mpCompatible-6.1, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.mpHealth-4.0, \
  io.openliberty.mpOpenAPI-3.1, \
  io.openliberty.mpFaultTolerance-4.0, \
  io.openliberty.mpJwt-2.1, \
  io.openliberty.mpMetrics-5.1, \
  io.openliberty.mpRestClient-3.0, \
  io.openliberty.mpTelemetry-1.1
kind=ga
edition=core
WLP-InstantOn-Enabled: true
