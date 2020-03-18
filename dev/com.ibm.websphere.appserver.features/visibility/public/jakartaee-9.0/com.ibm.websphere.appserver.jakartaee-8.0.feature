-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakartaee-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Platform 9.0
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  com.ibm.websphere.appserver.el-4.0, \
  com.ibm.websphere.appserver.jsp-3.0
kind=noship
edition=base
