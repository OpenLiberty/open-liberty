-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webProfile-10.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-10.0
Subsystem-Version: 10.0.0
Subsystem-Name: Jakarta EE Web Profile 10.0
-features=io.openliberty.cdi-4.0, \
  io.openliberty.faces-4.0, \
  io.openliberty.appSecurity-5.0, \
  io.openliberty.appAuthentication-3.0, \
  io.openliberty.jsonb-3.0, \
  io.openliberty.enterpriseBeansLite-4.0, \
  io.openliberty.websocket-2.1, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-3.1, \
  io.openliberty.beanValidation-3.0, \
  io.openliberty.managedBeans-2.0, \
  io.openliberty.restfulWS-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  io.openliberty.pages-3.1, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.expressionLanguage-5.0, \
  io.openliberty.jsonp-2.1
kind=noship
edition=full
