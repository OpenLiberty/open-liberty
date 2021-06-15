-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.containerServices-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-SPI-Package: \
 com.ibm.ws.container.service.annotations, \
 com.ibm.ws.container.service.app.deploy, \
 com.ibm.ws.container.service.config, \
 com.ibm.ws.container.service.naming, \
 com.ibm.ws.container.service.security, \
 com.ibm.ws.container.service.state, \
 com.ibm.ws.serialization, \
 com.ibm.wsspi.resource
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javaeedd-1.0, \
  com.ibm.websphere.appserver.anno-1.0; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.ws.resource, \
 com.ibm.ws.container.service, \
 com.ibm.ws.javaee.version, \
 com.ibm.ws.serialization
-jars=com.ibm.websphere.appserver.spi.containerServices; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.containerServices_4.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
