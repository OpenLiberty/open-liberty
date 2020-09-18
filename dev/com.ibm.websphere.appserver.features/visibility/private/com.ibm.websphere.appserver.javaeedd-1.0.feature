-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeedd-1.0
IBM-SPI-Package: com.ibm.ws.javaee.dd.appbnd, \
 com.ibm.ws.javaee.dd.common, \
 com.ibm.ws.javaee.dd.web, \
 com.ibm.ws.javaee.dd.web.common, \
 com.ibm.ws.javaee.dd.webbnd, \
 com.ibm.ws.javaee.dd.webext, \
 com.ibm.ws.javaee.dd.commonbnd, \
 com.ibm.ws.javaee.dd.commonext, \
 com.ibm.ws.javaee.dd.jsp
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.ws.javaee.ddmodel, \
 com.ibm.ws.javaee.dd, \
 com.ibm.ws.javaee.version, \
 com.ibm.ws.javaee.dd.common, \
 com.ibm.ws.javaee.dd.ejb
-jars=com.ibm.websphere.appserver.spi.javaeedd; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.javaeedd_1.4-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
