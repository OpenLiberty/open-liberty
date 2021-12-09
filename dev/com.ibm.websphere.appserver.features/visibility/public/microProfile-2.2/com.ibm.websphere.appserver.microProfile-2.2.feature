-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.microProfile-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-2.2
Subsystem-Version: 7.0.0
Subsystem-Name: MicroProfile 2.2
-features=com.ibm.websphere.appserver.mpConfig-1.3, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.mpJwt-1.1, \
  com.ibm.websphere.appserver.jaxrsClient-2.1, \
  com.ibm.websphere.appserver.mpRestClient-1.2, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.mpMetrics-1.1, \
  com.ibm.websphere.appserver.mpOpenAPI-1.1, \
  com.ibm.websphere.appserver.jsonb-1.0, \
  com.ibm.websphere.appserver.javax.annotation-1.3, \
  com.ibm.websphere.appserver.mpFaultTolerance-2.0, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  com.ibm.websphere.appserver.mpHealth-1.0, \
  com.ibm.websphere.appserver.mpOpenTracing-1.3
kind=ga
edition=core
