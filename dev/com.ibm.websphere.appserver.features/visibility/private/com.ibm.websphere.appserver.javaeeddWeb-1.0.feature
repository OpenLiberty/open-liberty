-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeeddWeb-1.0
IBM-SPI-Package: com.ibm.ws.javaee.dd.web, \
 com.ibm.ws.javaee.dd.web.common, \
 com.ibm.ws.javaee.dd.webbnd, \
 com.ibm.ws.javaee.dd.webext, \
 com.ibm.ws.javaee.dd.jsp
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javaeedd-1.0
-bundles=com.ibm.ws.javaee.ddmodel.web

kind=ga
edition=core
