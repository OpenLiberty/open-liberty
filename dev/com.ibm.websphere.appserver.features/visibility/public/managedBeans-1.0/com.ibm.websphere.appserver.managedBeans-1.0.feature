-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.managedBeans-1.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: managedBeans-1.0
Subsystem-Name: Java EE Managed Bean 1.0
-features=com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.javax.ejb-3.1; ibm.tolerates:=3.2; apiJar=false, \
 com.ibm.websphere.appserver.managedBeansCore-1.0, \
 com.ibm.websphere.appserver.javax.interceptor-1.1; ibm.tolerates:=1.2
-bundles=com.ibm.ws.managedbeans
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd
kind=ga
edition=core
WLP-Activation-Type: parallel
