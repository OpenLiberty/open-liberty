-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeans-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeans-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0
Subsystem-Category: JakartaEE9Application
-features= \
 io.openliberty.enterpriseBeansHome-4.0, \
 io.openliberty.enterpriseBeansLite-4.0, \
 com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:=4.3, \
 io.openliberty.mdb-4.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.enterpriseBeansRemote-4.0
kind=noship
edition=full
