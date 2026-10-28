-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.eventLogging-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: eventLogging-1.0
Subsystem-Name: Event Logging 1.0
-features=com.ibm.websphere.appserver.requestProbeServlet-1.0, \
  com.ibm.websphere.appserver.requestProbeJDBC-1.0
-bundles=com.ibm.ws.event.logging
kind=ga
edition=core
