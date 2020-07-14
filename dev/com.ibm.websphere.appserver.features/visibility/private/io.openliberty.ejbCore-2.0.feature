-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.ejbCore-2.0
IBM-API-Package: jakarta.ejb; type="spec", \
 jakarta.ejb.embeddable; type="spec", \
 jakarta.ejb.spi; type="spec"
-features=com.ibm.websphere.appserver.appmanager-1.0, \
 io.openliberty.jakartaeePlatform-9.0, \
 io.openliberty.managedBeansCore-2.0, \
 com.ibm.websphere.appserver.javaeeddSchema-1.0
-bundles=com.ibm.ws.app.manager.war.jakarta, \
 com.ibm.ws.app.manager.ejb
kind=noship
edition=full
WLP-Activation-Type: parallel
