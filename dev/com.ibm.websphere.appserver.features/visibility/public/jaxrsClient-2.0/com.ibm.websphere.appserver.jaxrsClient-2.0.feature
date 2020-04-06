-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsClient-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrsClient-2.0
Subsystem-Name: Java RESTful Services Client 2.0
-features=com.ibm.websphere.appserver.jaxrs.common-2.0, \
 com.ibm.websphere.appserver.javaeeCompatible-7.0
-bundles=com.ibm.ws.jaxrs.2.0.client, \
 com.ibm.ws.cxf.client
kind=ga
edition=core
WLP-Activation-Type: parallel
