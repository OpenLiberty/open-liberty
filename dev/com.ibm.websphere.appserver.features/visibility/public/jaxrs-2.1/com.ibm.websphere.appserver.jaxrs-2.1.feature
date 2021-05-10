-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs-2.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrs-2.1
Subsystem-Name: Java RESTful Services 2.1
-features=com.ibm.websphere.appserver.internal.jaxrs-2.1, \
  com.ibm.websphere.appserver.jaxrsClient-2.1, \
  com.ibm.websphere.appserver.eeCompatible-8.0
kind=ga
edition=core
WLP-Activation-Type: parallel
