-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.contextService-1.0
visibility=protected
IBM-API-Package: com.ibm.ws.context.service.serializable; type="internal"
-features=com.ibm.websphere.appserver.appLifecycle-1.0
-bundles=com.ibm.ws.resource, \
 com.ibm.ws.javaee.version, \
 com.ibm.ws.context
kind=ga
edition=core
WLP-Activation-Type: parallel
