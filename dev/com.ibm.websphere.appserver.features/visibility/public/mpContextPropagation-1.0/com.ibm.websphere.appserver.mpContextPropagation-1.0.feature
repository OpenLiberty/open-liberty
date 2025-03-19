-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpContextPropagation-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: mpContextPropagation-1.0
Subsystem-Name: MicroProfile Context Propagation 1.0
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: \
  org.eclipse.microprofile.context; type="stable", \
  org.eclipse.microprofile.context.spi; type="stable"
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.concurrent-1.0
-bundles=\
  com.ibm.ws.microprofile.contextpropagation.1.0
kind=ga
edition=core
WLP-Platform: microProfile-1.0,microProfile-1.2,microProfile-1.3,microProfile-1.4,microProfile-2.0,microProfile-2.1,microProfile-2.2,microProfile-3.0,microProfile-3.2,microProfile-3.3
