-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeeClient-8.0
visibility=public
IBM-API-Package: com.ibm.ws.ejb.portable; type="internal", \
 javax.xml.ws; type="internal"
IBM-ShortName: javaeeClient-8.0
Subsystem-Name: Java EE V8 Application Client
-features=com.ibm.websphere.appserver.ejbRemoteClient-1.0, \
 com.ibm.websphere.appserver.cdi-2.0, \
 com.ibm.websphere.appserver.jndiClient-1.0, \
 com.ibm.websphere.appserver.jpa-2.2, \
 com.ibm.websphere.appserver.beanValidation-2.0, \
 com.ibm.websphere.appserver.jaxwsClient-2.2, \
 com.ibm.websphere.appserver.j2eeManagementClient-1.1, \
 com.ibm.websphere.appserver.jsonp-1.1, \
 com.ibm.websphere.appserver.jsonb-1.0, \
 com.ibm.websphere.appserver.javaMail-1.6, \
 com.ibm.websphere.appserver.jaxb-2.2, \
 com.ibm.websphere.appclient.appClient-1.0, \
 com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2", \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.managedBeans-1.0, \
 com.ibm.websphere.appserver.wasJmsClient-2.0
kind=noship
edition=base
