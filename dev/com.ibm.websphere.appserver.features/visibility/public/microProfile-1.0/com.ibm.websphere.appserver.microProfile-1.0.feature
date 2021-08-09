-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.microProfile-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: microProfile-1.0
Subsystem-Version: 7.0.0
Subsystem-Name: MicroProfile 1.0
-features=com.ibm.websphere.appserver.jsonp-1.0, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.cdi-1.2, \
  com.ibm.websphere.appserver.jaxrs-2.0
kind=ga
edition=core
