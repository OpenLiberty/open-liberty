-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webProfile-7.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: webProfile-7.0
Subsystem-Version: 7.0.0
Subsystem-Name: Java EE Web Profile 7.0
-features=com.ibm.websphere.appserver.appSecurity-2.0, \
  com.ibm.websphere.appserver.jsonp-1.0, \
  com.ibm.websphere.appserver.el-3.0, \
  com.ibm.websphere.appserver.beanValidation-1.1, \
  com.ibm.websphere.appserver.cdi-1.2, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.websocket-1.1, \
  com.ibm.websphere.appserver.servlet-3.1, \
  com.ibm.websphere.appserver.jpa-2.1, \
  com.ibm.websphere.appserver.jsp-2.3, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.ejbLite-3.2, \
  com.ibm.websphere.appserver.managedBeans-1.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.jaxrs-2.0, \
  com.ibm.websphere.appserver.jsf-2.2
kind=ga
edition=core
