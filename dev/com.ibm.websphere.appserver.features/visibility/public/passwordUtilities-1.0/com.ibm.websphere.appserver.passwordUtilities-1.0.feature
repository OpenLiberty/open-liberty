-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.passwordUtilities-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: passwordUtilities-1.0
IBM-API-Package: \
 com.ibm.websphere.security.jca; type="ibm-api", \
 com.ibm.websphere.crypto; type="ibm-api", \
 com.ibm.websphere.security.auth.data; type="ibm-api"
Subsystem-Name: Password Utilities 1.0
-features=\
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0, 5.0", \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2,2.0", \
 io.openliberty.passwordUtilities1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-jars=\
 com.ibm.websphere.appserver.api.passwordUtil; location:=dev/api/ibm/, \
 com.ibm.websphere.appserver.api.authData; location:=dev/api/ibm/, \
 io.openliberty.authData; location:=dev/api/ibm/
-files=\
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.passwordUtil_1.0-javadoc.zip
kind=ga
edition=base
