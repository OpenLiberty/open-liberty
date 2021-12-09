-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.microProfile-1.4
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-1.4
Subsystem-Version: 7.0.0
Subsystem-Name: MicroProfile 1.4
-features=com.ibm.websphere.appserver.mpOpenTracing-1.1, \
  com.ibm.websphere.appserver.mpConfig-1.3, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:="1.1", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.mpJwt-1.1, \
  com.ibm.websphere.appserver.mpRestClient-1.1, \
  com.ibm.websphere.appserver.jaxrsClient-2.0; ibm.tolerates:="2.1", \
  com.ibm.websphere.appserver.mpMetrics-1.1, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.mpOpenAPI-1.0, \
  com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:="1.3", \
  com.ibm.websphere.appserver.mpFaultTolerance-1.1, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:="2.1", \
  com.ibm.websphere.appserver.mpHealth-1.0
kind=ga
edition=core
