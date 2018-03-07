-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBootWebSupport-1.0
visibility=private
-features= \
 com.ibm.websphere.appserver.springBootHandler-1.0
 com.ibm.websphere.appserver.javax.servlet-3.1; ibm.tolerates:="4.0"
-bundles=com.ibm.ws.springboot.support.web
kind=noship
edition=core
IBM-API-Package: com.ibm.ws.springboot.support.web.initializer; type="internal"

