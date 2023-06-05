-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.passwordUtilities-1.1
visibility=public
singleton=true
IBM-ShortName: passwordUtilities-1.1
IBM-API-Package: \
 com.ibm.websphere.security.jca; type="ibm-api", \
 com.ibm.websphere.crypto; type="ibm-api", \
 com.ibm.websphere.security.auth.data; type="ibm-api"
Subsystem-Name: Password Utilities 1.1
-features=\
  io.openliberty.passwordUtilities1.1.internal.ee-6.0; ibm.tolerates:="8.0,9.0,10.0", \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2,2.0"
-jars=\
 com.ibm.websphere.appserver.api.passwordUtil; location:=dev/api/ibm/, \
 com.ibm.websphere.appserver.api.authData; location:=dev/api/ibm/
-files=\
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.authData_1.0-javadoc.zip, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.passwordUtil_1.0-javadoc.zip
kind=ga
edition=core
WLP-InstantOn-Enabled: true
