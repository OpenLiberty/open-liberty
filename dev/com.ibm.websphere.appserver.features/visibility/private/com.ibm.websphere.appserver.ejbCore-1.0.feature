-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbCore-1.0
IBM-API-Package: javax.ejb; type="spec", \
 javax.ejb.embeddable; type="spec", \
 javax.ejb.spi; type="spec"
-features=com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.javaeePlatform-6.0, \
 com.ibm.websphere.appserver.managedBeansCore-1.0, \
 com.ibm.websphere.appserver.javaeeddSchema-1.0
-bundles=com.ibm.ws.app.manager.war, \
 com.ibm.ws.app.manager.ejb
kind=ga
edition=core
WLP-Activation-Type: parallel
