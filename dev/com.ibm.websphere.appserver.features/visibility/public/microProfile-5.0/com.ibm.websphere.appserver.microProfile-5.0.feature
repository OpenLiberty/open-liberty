-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.microProfile-5.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-5.0
Subsystem-Name: MicroProfile 5.0
-features=\
  io.openliberty.cdi-3.0, \
  io.openliberty.jsonb-2.0, \
  io.openliberty.jsonp-2.0, \
  io.openliberty.restfulWS-3.0, \
  io.openliberty.restfulWSClient-3.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.mpCompatible-5.0, \
  com.ibm.websphere.appserver.mpConfig-3.0, \
  io.openliberty.mpJwt-2.0
  #com.ibm.websphere.appserver.mpFaultTolerance-4.0, \
  #com.ibm.websphere.appserver.mpHealth-4.0, \
  #com.ibm.websphere.appserver.mpMetrics-4.0, \
  #com.ibm.websphere.appserver.mpOpenAPI-3.0, \
  #com.ibm.websphere.appserver.mpOpenTracing-3.0, \
  #com.ibm.websphere.appserver.mpRestClient-3.0
kind=noship
edition=full
