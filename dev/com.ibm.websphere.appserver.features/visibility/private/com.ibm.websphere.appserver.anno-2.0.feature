-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.anno-2.0
singleton=true
IBM-API-Package: jakarta.annotation; type="spec", \
 jakarta.annotation.security; type="spec", \
 jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
 com.ibm.wsspi.anno.classsource, \
 com.ibm.wsspi.anno.info, \
 com.ibm.wsspi.anno.service, \
 com.ibm.wsspi.anno.targets, \
 com.ibm.wsspi.anno.util
Manifest-Version: 1.0
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.annotation-2.0, \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.ws.anno
-jars=com.ibm.websphere.appserver.spi.anno; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.anno_1.1-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
