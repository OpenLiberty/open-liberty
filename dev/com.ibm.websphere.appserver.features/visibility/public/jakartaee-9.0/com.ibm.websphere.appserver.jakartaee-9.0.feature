-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakartaee-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Platform 9.0
-features=\
 com.ibm.websphere.appserver.el-4.0, \
 com.ibm.websphere.appserver.jsp-3.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 com.ibm.websphere.appserver.jsonp-2.0, \
 com.ibm.websphere.appserver.jsonb-2.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 com.ibm.websphere.appserver.jndi-1.0, \
 io.openliberty.cdi-3.0, \
 com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3"
kind=beta
edition=core
