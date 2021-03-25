-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webProfile-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Web Profile 9.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3",\
  com.ibm.websphere.appserver.jndi-1.0,\
  com.ibm.websphere.appserver.servlet-5.0,\
  com.ibm.websphere.appserver.transaction-2.0,\
  io.openliberty.appAuthentication-2.0,\
  io.openliberty.appSecurity-4.0,\
  io.openliberty.beanValidation-3.0,\
  io.openliberty.cdi-3.0,\
  io.openliberty.expressionLanguage-4.0,\
  io.openliberty.enterpriseBeansLite-4.0,\
  io.openliberty.faces-3.0,\
  io.openliberty.persistence-3.0,\
  io.openliberty.jsonb-2.0,\
  io.openliberty.jsonp-2.0,\
  io.openliberty.pages-3.0,\
  io.openliberty.managedBeans-2.0,\
  io.openliberty.restfulWS-3.0,\
  io.openliberty.websocket-2.0
kind=beta
edition=core
