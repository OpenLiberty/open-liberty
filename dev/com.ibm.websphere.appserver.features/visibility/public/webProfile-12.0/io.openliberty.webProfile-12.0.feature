-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webProfile-12.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-12.0
Subsystem-Version: 12.0.0
Subsystem-Name: Jakarta EE Web Profile 12.0
-features=io.openliberty.cdi-5.0, \
  io.openliberty.faces-5.0, \
  io.openliberty.appSecurity-7.0, \
  io.openliberty.appAuthentication-3.1, \
  io.openliberty.jsonb-3.1, \
  io.openliberty.enterpriseBeansLite-4.0, \
  io.openliberty.websocket-2.3, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-4.0, \
  io.openliberty.beanValidation-4.0, \
  io.openliberty.restfulWS-5.0, \
  io.openliberty.concurrent-3.2, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.pages-4.1, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.transaction-2.1, \
  io.openliberty.expressionLanguage-6.1, \
  io.openliberty.jsonp-2.2, \
  io.openliberty.data-1.1
kind=noship
edition=full
WLP-InstantOn-Enabled: true
