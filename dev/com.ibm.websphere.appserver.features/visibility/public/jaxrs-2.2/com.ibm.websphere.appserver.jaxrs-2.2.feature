-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs-2.2
visibility=public
singleton=true
IBM-API-Package: com.ibm.websphere.jaxrs.server; type="ibm-api", \
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrs-2.2
Subsystem-Name: Java RESTful Web Services 2.1 (RESTEasy)
-features=\
 com.ibm.websphere.appserver.internal.jaxrs-2.2, \
 com.ibm.websphere.appserver.jaxrsClient-2.2, \
 com.ibm.websphere.appserver.eeCompatible-8.0, \
 com.ibm.websphere.appserver.cdi-2.0
kind=noship
edition=full
WLP-Activation-Type: parallel
