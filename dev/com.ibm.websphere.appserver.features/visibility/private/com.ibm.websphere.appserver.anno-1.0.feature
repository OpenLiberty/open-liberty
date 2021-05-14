-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.anno-1.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: javax.annotation; type="spec", \
 javax.annotation.security; type="spec", \
 javax.annotation.sql; type="spec"
IBM-SPI-Package: \
 com.ibm.wsspi.anno.classsource, \
 com.ibm.wsspi.anno.info, \
 com.ibm.wsspi.anno.service, \
 com.ibm.wsspi.anno.targets, \
 com.ibm.wsspi.anno.util
Manifest-Version: 1.0
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.javax.annotation-1.1; ibm.tolerates:="1.2,1.3", \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.ws.anno
-jars=com.ibm.websphere.appserver.spi.anno; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.anno_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
