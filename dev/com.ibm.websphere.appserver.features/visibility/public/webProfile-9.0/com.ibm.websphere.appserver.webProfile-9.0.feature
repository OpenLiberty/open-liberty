-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webProfile-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Web Profile 9.0
-features=\
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3",\
  com.ibm.websphere.appserver.jndi-1.0,\
  com.ibm.websphere.appserver.jsonb-2.0,\
  com.ibm.websphere.appserver.jsonp-2.0,\
  com.ibm.websphere.appserver.servlet-5.0,\
  com.ibm.websphere.appserver.transaction-2.0,\
  io.openliberty.beanValidation-3.0,\
  io.openliberty.cdi-3.0,\
  io.openliberty.ejbLite-4.0,\
  io.openliberty.el-4.0,\
  io.openliberty.jsp-3.0,\
  io.openliberty.managedBeans-2.0,\
  io.openliberty.websocket-2.0
kind=beta
edition=core
