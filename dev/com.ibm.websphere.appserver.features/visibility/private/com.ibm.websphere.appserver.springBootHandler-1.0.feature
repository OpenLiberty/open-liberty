-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBootHandler-1.0
visibility=private
-features= \
 com.ibm.websphere.appserver.artifact-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.javaeePlatform-7.0
-bundles=com.ibm.ws.app.manager.springboot, \
 com.ibm.ws.springboot.support.shutdown
kind=noship
edition=core
IBM-API-Package: com.ibm.ws.app.manager.springboot.container.config; type="internal", \
 com.ibm.ws.app.manager.springboot.container; type="internal"