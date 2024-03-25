-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.openapi-3.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: openapi-3.1
IBM-API-Package: \
  com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.openapi31; type="ibm-spi"
 
Subsystem-Name: OpenAPI 3.1

-features=com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  io.openliberty.servlet.internal-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.adminSecurity-1.0, \
  io.openliberty.securityAPI.javaee-1.0, \
  com.ibm.websphere.appserver.mpOpenAPI-1.0, \
  com.ibm.websphere.appserver.mpConfig-1.2; ibm.tolerates:="1.3,1.4", \
  com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:="2.1"

-bundles= \
 com.ibm.websphere.openapi.3.1, \
 com.ibm.ws.openapi.3.1, \
 com.ibm.ws.openapi.3.1.private, \
 com.ibm.ws.openapi.3.1.public, \
 com.ibm.ws.openapi.ui, \
 com.ibm.ws.openapi.ui.private, \
 com.ibm.ws.org.apache.commons.io; location:=lib/, \
 com.ibm.ws.org.apache.commons.lang3; location:=lib/

-jars= \
 com.ibm.websphere.appserver.spi.openapi.3.1; location:=dev/spi/ibm/

-files=\
  dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.openapi.3.1_1.0-javadoc.zip

kind=ga
edition=core
