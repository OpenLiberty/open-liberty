-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.microProfile-6.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-6.0
Subsystem-Name: MicroProfile 6.0
-features=\
  io.openliberty.noShip-1.0, \
  io.openliberty.cdi-4.0, \
  io.openliberty.jsonb-3.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.restfulWS-3.1, \
  io.openliberty.restfulWSClient-3.1, \
  com.ibm.websphere.appserver.servlet-6.0, \
  io.openliberty.mpCompatible-6.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.mpHealth-4.0, \
  io.openliberty.mpOpenAPI-3.1, \
  io.openliberty.mpFaultTolerance-4.0, \
  io.openliberty.mpMetrics-5.0, \
  io.openliberty.mpRestClient-3.0, \
  io.openliberty.mpTelemetry-1.0
kind=noship
edition=full
