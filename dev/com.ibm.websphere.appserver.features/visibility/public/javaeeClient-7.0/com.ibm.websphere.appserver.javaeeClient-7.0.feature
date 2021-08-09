-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeeClient-7.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: com.ibm.ws.ejb.portable; type="internal", \
 javax.xml.ws; type="internal"
IBM-ShortName: javaeeClient-7.0
Subsystem-Name: Java EE 7 Application Client
-features=com.ibm.websphere.appserver.jsonp-1.0, \
  com.ibm.websphere.appserver.jndiClient-1.0, \
  com.ibm.websphere.appserver.beanValidation-1.1, \
  com.ibm.websphere.appserver.jaxwsClient-2.2, \
  com.ibm.websphere.appserver.cdi-1.2, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.j2eeManagementClient-1.1, \
  com.ibm.websphere.appserver.jaxb-2.2, \
  com.ibm.websphere.appserver.jpa-2.1, \
  com.ibm.websphere.appclient.appClient-1.0, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.wasJmsClient-2.0, \
  com.ibm.websphere.appserver.ejbRemoteClient-1.0, \
  com.ibm.websphere.appserver.javaMail-1.5, \
  com.ibm.websphere.appserver.managedBeans-1.0
kind=ga
edition=base
