-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webProfile-11.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-11.0
Subsystem-Version: 11.0.0
Subsystem-Name: Jakarta EE Web Profile 11.0
-features=io.openliberty.cdi-4.1, \
  io.openliberty.faces-4.1, \
  io.openliberty.appSecurity-6.0, \
  io.openliberty.appAuthentication-3.1, \
  io.openliberty.jsonb-3.0, \
  io.openliberty.enterpriseBeansLite-4.0, \
  io.openliberty.websocket-2.2, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-3.2, \
  io.openliberty.beanValidation-3.1, \
  io.openliberty.restfulWS-4.0, \
  io.openliberty.concurrent-3.1, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.pages-4.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.expressionLanguage-6.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.data-1.0
kind=noship
edition=full
WLP-InstantOn-Enabled: true
