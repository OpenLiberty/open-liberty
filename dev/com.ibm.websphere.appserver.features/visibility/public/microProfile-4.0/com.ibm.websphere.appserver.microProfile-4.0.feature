-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.microProfile-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-4.0
Subsystem-Version: 7.0.0
Subsystem-Name: MicroProfile 4.0
-features=com.ibm.websphere.appserver.mpOpenTracing-2.0, \
  com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.mpConfig-2.0, \
  com.ibm.websphere.appserver.mpJwt-1.2, \
  com.ibm.websphere.appserver.mpRestClient-2.0, \
  com.ibm.websphere.appserver.jaxrsClient-2.1, \
  io.openliberty.mpCompatible-4.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.mpOpenAPI-2.0, \
  com.ibm.websphere.appserver.jsonb-1.0, \
  com.ibm.websphere.appserver.mpMetrics-3.0, \
  com.ibm.websphere.appserver.mpFaultTolerance-3.0, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  com.ibm.websphere.appserver.mpHealth-3.0
kind=ga
edition=core
