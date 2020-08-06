-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxrs-3.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.server; type="ibm-api", \
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrs-3.0
Subsystem-Name: Java RESTful Web Services 3.0
-features=\
 io.openliberty.jaxrsClient-3.0, \
 io.openliberty.internal.jaxrs-3.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 io.openliberty.cdi-3.0
kind=noship
edition=full
WLP-Activation-Type: parallel
