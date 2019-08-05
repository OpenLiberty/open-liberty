-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaee-8.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: javaee-8.0
Subsystem-Version: 8.0.0
Subsystem-Name: Java EE Full Platform 8.0
-features=\
  com.ibm.websphere.appserver.appClientSupport-1.0,\
  com.ibm.websphere.appserver.batch-1.0,\
  com.ibm.websphere.appserver.concurrent-1.0,\
  com.ibm.websphere.appserver.ejb-3.2,\
  com.ibm.websphere.appserver.jacc-1.5,\
  com.ibm.websphere.appserver.javaMail-1.6,\
  com.ibm.websphere.appserver.javax.persistence.base-2.2,\
  com.ibm.websphere.appserver.jaxws-2.2,\
  com.ibm.websphere.appserver.jca-1.7,\
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3",\
  com.ibm.websphere.appserver.jcaInboundSecurity-1.0,\
  com.ibm.websphere.appserver.jms-2.0,\
  com.ibm.websphere.appserver.j2eeManagement-1.1,\
  com.ibm.websphere.appserver.servlet-4.0,\
  com.ibm.websphere.appserver.transaction-1.2,\
  com.ibm.websphere.appserver.wasJmsClient-2.0,\
  com.ibm.websphere.appserver.wasJmsSecurity-1.0,\
  com.ibm.websphere.appserver.wasJmsServer-1.0,\
  com.ibm.websphere.appserver.webProfile-8.0
kind=ga
edition=base
WLP-Activation-Type: parallel
