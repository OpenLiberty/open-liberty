-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webProfile-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: webProfile-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Web Profile 9.0
-features=\
  com.ibm.websphere.appserver.el-4.0,\
  com.ibm.websphere.appserver.jsonb-2.0,\
  com.ibm.websphere.appserver.jsonp-2.0,\
  com.ibm.websphere.appserver.jsp-3.0,\
  com.ibm.websphere.appserver.servlet-5.0,\
  com.ibm.websphere.appserver.transaction-2.0
kind=noship
edition=full
