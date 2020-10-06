-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.passwordUtilities-1.0
visibility=public
IBM-ShortName: passwordUtilities-1.0
IBM-API-Package: \
 com.ibm.websphere.security.jca; type="ibm-api", \
 com.ibm.websphere.crypto; type="ibm-api", \
 com.ibm.websphere.security.auth.data; type="ibm-api"
Subsystem-Name: Password Utilities 1.0
-features=\
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0, 5.0"
-jars=\
 com.ibm.websphere.appserver.api.passwordUtil; location:=dev/api/ibm/
-files=\
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.passwordUtil_1.0-javadoc.zip
kind=ga
edition=base
