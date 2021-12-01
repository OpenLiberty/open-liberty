-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webCache-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.servlet.cache; type="ibm-api", \
 com.ibm.websphere.command; type="ibm-api", \
 com.ibm.websphere.command.web; type="ibm-api", \
 com.ibm.ws.cache.servlet; type="internal", \
 com.ibm.ws.cache.web; type="internal", \
 com.ibm.ws.cache.web.command; type="internal", \
 com.ibm.ws.cache.command; type="internal", \
 com.ibm.ws.cache.web.config; type="internal"
IBM-ShortName: webCache-1.0
IBM-SPI-Package: com.ibm.wsspi.cache.web
Subsystem-Name: Web Response Cache 1.0
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  com.ibm.websphere.appserver.distributedMap-1.0, \
  io.openliberty.webCache1.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0"
-jars=com.ibm.websphere.appserver.spi.webCache; location:=dev/spi/ibm/, \
 com.ibm.websphere.appserver.api.webCache; location:=dev/api/ibm/, \
 io.openliberty.webCache; location:=dev/api/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.webCache_1.0-javadoc.zip, \
 dev/api/ibm/schema/cachespec.xsd, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.webCache_1.1-javadoc.zip, \
 dev/api/ibm/javadoc/io.openliberty.webCache_1.1-javadoc.zip
kind=ga
edition=core
