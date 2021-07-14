-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.passwordUtilities1.0.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
  com.ibm.websphere.appserver.javax.connector-1.7, \
  com.ibm.websphere.appserver.appSecurity-1.0; ibm.tolerates:="2.0,3.0", \
  com.ibm.websphere.appserver.authData-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2"
-files=\
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.authData_1.0-javadoc.zip
kind=ga
edition=base
