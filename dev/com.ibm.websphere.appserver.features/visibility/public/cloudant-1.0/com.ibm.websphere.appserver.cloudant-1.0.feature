-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cloudant-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: cloudant-1.0
Subsystem-Name: Cloudant Integration 1.0
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.ws.cloudant, \
 com.ibm.ws.security.auth.data.common
kind=ga
edition=base
