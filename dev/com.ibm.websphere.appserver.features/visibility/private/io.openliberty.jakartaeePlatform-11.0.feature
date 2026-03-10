-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaeePlatform-11.0
singleton=true
IBM-Process-Types: client, server
-features=com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.eeCompatible-11.0
# com.ibm.ws.security.java2sec is removed for EE 11
-bundles=com.ibm.ws.app.manager.module, \
 com.ibm.ws.javaee.platform.defaultresource
kind=ga
edition=core
WLP-Activation-Type: parallel
