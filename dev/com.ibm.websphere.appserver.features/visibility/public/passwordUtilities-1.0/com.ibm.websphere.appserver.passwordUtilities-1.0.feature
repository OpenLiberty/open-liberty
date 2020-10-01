-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.passwordUtilities-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: passwordUtilities-1.0
IBM-API-Package: com.ibm.websphere.security.jca; type="ibm-api", \
 com.ibm.websphere.crypto; type="ibm-api", \
 com.ibm.websphere.security.auth.data; type="ibm-api",
Subsystem-Name: Password Utilities 1.0
-features=com.ibm.websphere.appserver.authData-1.0, \
 com.ibm.websphere.appserver.appSecurity-1.0; ibm.tolerates:="2.0, 3.0", \
 com.ibm.websphere.appserver.javax.connector-1.6; ibm.tolerates:=1.7, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0", \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2
-jars=com.ibm.websphere.appserver.api.authData; location:=dev/api/ibm/, \
 com.ibm.websphere.appserver.api.passwordUtil; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.authData_1.0-javadoc.zip, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.passwordUtil_1.0-javadoc.zip
kind=ga
edition=base
