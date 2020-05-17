-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrs-3.0
Subsystem-Name: Java RESTful Services 3.0
-features=\
 com.ibm.websphere.appserver.internal.jaxrs-3.0, \
 com.ibm.websphere.appserver.jaxrsClient-3.0, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0, \
 com.ibm.websphere.appserver.cdi-2.0
kind=noship
edition=full
WLP-Activation-Type: parallel
