-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenTracing-1.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-1.2
Subsystem-Name: MicroProfile OpenTracing 1.2
IBM-API-Package: \
  org.eclipse.microprofile.opentracing; type="stable"
-features=com.ibm.websphere.appserver.opentracing-1.2, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-1.2, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:="2.1"
-bundles=\
  com.ibm.ws.microprofile.opentracing.1.2
kind=ga
edition=core
WLP-Activation-Type: parallel
