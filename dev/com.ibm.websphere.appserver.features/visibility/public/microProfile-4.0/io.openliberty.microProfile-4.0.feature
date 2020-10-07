-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.microProfile-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-4.0
Subsystem-Version: 7.0.0
Subsystem-Name: MicroProfile 4.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  com.ibm.websphere.appserver.jaxrsClient-2.1, \
  com.ibm.websphere.appserver.jsonb-1.0, \
  com.ibm.websphere.appserver.jsonp-1.1, \
  io.openliberty.mpConfig-2.0, \
  io.openliberty.mpFaultTolerance-3.0, \
  io.openliberty.mpHealth-3.0, \
  io.openliberty.mpJwt-1.2, \
  io.openliberty.mpMetrics-3.0, \
  io.openliberty.mpOpenAPI-2.0, \
  io.openliberty.mpOpenTracing-2.0
kind=beta
edition=core
