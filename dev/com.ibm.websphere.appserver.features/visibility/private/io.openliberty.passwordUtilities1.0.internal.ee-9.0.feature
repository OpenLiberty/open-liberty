-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.passwordUtilities1.0.internal.ee-9.0
singleton=true
-features=\
 com.ibm.websphere.appserver.authData-2.0, \
 io.openliberty.appSecurity-4.0, \
 io.openliberty.appserver.connectors-2.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 com.ibm.websphere.appserver.transaction-2.0
-files=\
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.authData_1.0-javadoc.zip
kind=beta
edition=base
