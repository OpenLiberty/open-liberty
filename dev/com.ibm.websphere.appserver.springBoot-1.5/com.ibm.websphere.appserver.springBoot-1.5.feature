-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.springBoot-1.5
visibility=public
singleton=true
IBM-ShortName: springBoot-1.5
IBM-Process-Types: client, \
 server
Subsystem-Name: Spring Boot Support version 1.5
-features=com.ibm.websphere.appserver.springBootWebSupport-1.0, \
 com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0"
-bundles=com.ibm.ws.springboot.support.web.version15
kind=noship
edition=core
IBM-API-Package: com.ibm.ws.springboot.support.web.initializer; type="internal"
