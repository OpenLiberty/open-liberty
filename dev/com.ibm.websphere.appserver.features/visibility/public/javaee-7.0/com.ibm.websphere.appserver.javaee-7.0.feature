-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaee-7.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: javaee-7.0
Subsystem-Version: 7.0.0
Subsystem-Name: Java EE Full Platform 7.0
-features=com.ibm.websphere.appserver.jcaInboundSecurity-1.0, \
  com.ibm.websphere.appserver.batch-1.0, \
  com.ibm.websphere.appserver.jms-2.0, \
  com.ibm.websphere.appserver.wasJmsSecurity-1.0, \
  com.ibm.websphere.appserver.wasJmsServer-1.0, \
  com.ibm.websphere.appserver.concurrent-1.0, \
  com.ibm.websphere.appserver.ejb-3.2, \
  com.ibm.websphere.appserver.jaspic-1.1, \
  com.ibm.websphere.appserver.appClientSupport-1.0, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jca-1.7, \
  com.ibm.websphere.appserver.jaxws-2.2, \
  com.ibm.websphere.appserver.jacc-1.5, \
  com.ibm.websphere.appserver.servlet-3.1, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.wasJmsClient-2.0, \
  com.ibm.websphere.appserver.javaMail-1.5, \
  com.ibm.websphere.appserver.j2eeManagement-1.1, \
  com.ibm.websphere.appserver.webProfile-7.0
kind=ga
edition=base
