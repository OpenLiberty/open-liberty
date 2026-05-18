-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaeePlatform-12.0
singleton=true
IBM-Process-Types: client, server
-features=com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.noShip-1.0
# com.ibm.ws.security.java2sec is removed for EE 11
-bundles=com.ibm.ws.app.manager.module, \
 com.ibm.ws.javaee.platform.defaultresource
kind=noship
edition=full
WLP-Activation-Type: parallel
