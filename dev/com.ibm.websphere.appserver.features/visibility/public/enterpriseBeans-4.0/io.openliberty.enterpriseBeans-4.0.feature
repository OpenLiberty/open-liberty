-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeans-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeans-4.0
WLP-AlsoKnownAs: ejb-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0
Subsystem-Category: JakartaEE9Application
# io.openliberty.enterpriseBeansHome-4.0 is enabled conditionally by io.openliberty.enterpriseBeans4.0.internal.ee-9.0
# but not enabled for EE 11 since it is not part of EE 11 spec by default which requires the user to enable it explicitly
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0,11.0", \
  io.openliberty.enterpriseBeans4.0.internal.ee-9.0; ibm.tolerates:="11.0", \
  io.openliberty.enterpriseBeansRemote-4.0, \
  io.openliberty.enterpriseBeansPersistentTimer-4.0, \
  io.openliberty.mdb-4.0, \
  io.openliberty.enterpriseBeansLite-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-9.1,jakartaee-10.0,jakartaee-11.0
