-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenTracing-1.3
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-1.3
Subsystem-Name: MicroProfile OpenTracing 1.3
IBM-API-Package: \
  org.eclipse.microprofile.opentracing; type="stable"
-features=com.ibm.websphere.appserver.opentracing-1.3, \
  com.ibm.websphere.appserver.mpConfig-1.3; ibm.tolerates:="1.4", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-1.3
-bundles=\
  com.ibm.ws.microprofile.opentracing.1.3
kind=ga
edition=core
WLP-Activation-Type: parallel
