-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeans-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeans-4.0
WLP-AlsoKnownAs: ejb-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0
Subsystem-Category: JakartaEE9Application
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0,11.0", \
  io.openliberty.enterpriseBeansRemote-4.0, \
  io.openliberty.enterpriseBeansPersistentTimer-4.0, \
  io.openliberty.mdb-4.0, \
  io.openliberty.enterpriseBeansHome-4.0, \
  io.openliberty.enterpriseBeansLite-4.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.connectors-2.0; ibm.tolerates:="2.1"
kind=ga
edition=base
WLP-Activation-Type: parallel
