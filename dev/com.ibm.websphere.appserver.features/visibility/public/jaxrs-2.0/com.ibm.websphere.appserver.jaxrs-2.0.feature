-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrs-2.0
Subsystem-Name: Java RESTful Services 2.0
-features=com.ibm.websphere.appserver.internal.jaxrs-2.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0, \
  com.ibm.websphere.appserver.jaxrsClient-2.0
kind=ga
edition=core
WLP-Activation-Type: parallel
