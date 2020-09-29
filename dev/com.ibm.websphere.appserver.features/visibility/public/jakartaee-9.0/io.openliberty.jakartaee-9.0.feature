-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaee-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Platform 9.0
-features=\
 com.ibm.websphere.appserver.adminSecurity-2.0,\
 com.ibm.websphere.appserver.concurrent-2.0,\
 com.ibm.websphere.appserver.restConnector-2.0,\
 com.ibm.websphere.appserver.servlet-5.0,\
 com.ibm.websphere.appserver.transaction-2.0,\
 io.openliberty.enterpriseBeans-4.0,\
 io.openliberty.appClientSupport-2.0,\
 io.openliberty.connectors-2.0,\
 io.openliberty.jacc-2.0,\
 io.openliberty.jaxb-3.0,\
 io.openliberty.webProfile-9.0
kind=beta
edition=base
