-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaeeClient-10.0
visibility=public
singleton=true
IBM-ShortName: jakartaeeClient-10.0
Subsystem-Name: Jakarta EE 10.0 Application Client
-features=io.openliberty.jakartaeePlatform-10.0, \
  io.openliberty.cdi-4.0, \
  io.openliberty.enterpriseBeansRemoteClient-2.0, \
  io.openliberty.mail-2.1, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.jakarta.jndiClient-2.0, \
  io.openliberty.jsonb-3.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-3.1, \
  io.openliberty.xmlBinding-4.0, \
  io.openliberty.beanValidation-3.0, \
  io.openliberty.managedBeans-2.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.appclient.appClient-2.0, \
  io.openliberty.xmlWSClient-4.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.expressionLanguage-5.0
kind=noship
edition=full
