-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.managedBeans-2.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: managedBeans-2.0
Subsystem-Name: Jakarta Managed Beans 2.0
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.interceptor-2.0, \
  io.openliberty.jakarta.enterpriseBeans-4.0; apiJar=false, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.managedBeansCore-2.0
-bundles=com.ibm.ws.managedbeans
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd
kind=beta
edition=core
WLP-Activation-Type: parallel
