-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mdb-3.2
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mdb-3.2
IBM-API-Package: com.ibm.ws.ejbcontainer.mdb; type="internal"
Subsystem-Category: JavaEE7Application
-features=com.ibm.websphere.appserver.javaeePlatform-7.0, \
 com.ibm.websphere.appserver.ejbCore-1.0, \
 com.ibm.websphere.appserver.javax.ejb-3.2, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.jca-1.7, \
 com.ibm.websphere.appserver.javax.interceptor-1.2
-bundles=com.ibm.ws.ejbcontainer.mdb, \
 com.ibm.ws.ejbcontainer.v32
Subsystem-Name: Message-Driven Beans 3.2
kind=ga
edition=base
WLP-Activation-Type: parallel
