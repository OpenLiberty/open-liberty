-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.securityAPI.javaee-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-jars=com.ibm.websphere.appserver.api.security; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security_1.3-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
