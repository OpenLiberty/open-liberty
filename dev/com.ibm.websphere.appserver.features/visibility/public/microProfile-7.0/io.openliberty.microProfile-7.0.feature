-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.microProfile-7.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-7.0
Subsystem-Name: MicroProfile 7.0
-features=\
  io.openliberty.cdi-4.0; ibm.tolerates:="4.1", \
  io.openliberty.jsonb-3.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.restfulWS-3.1; ibm.tolerates:="4.0", \
  io.openliberty.restfulWSClient-3.1; ibm.tolerates:="4.0", \
  io.openliberty.mpCompatible-7.0, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.mpHealth-4.0, \
  io.openliberty.mpOpenAPI-4.0, \
  io.openliberty.mpFaultTolerance-4.1, \
  io.openliberty.mpJwt-2.1, \
  io.openliberty.mpRestClient-4.0, \
  io.openliberty.mpTelemetry-2.0
kind=ga
edition=core
WLP-InstantOn-Enabled: true
