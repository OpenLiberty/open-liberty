-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.restHandler-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-App-ForceRestart: uninstall, \
 install
IBM-SPI-Package: com.ibm.wsspi.rest.handler; type="ibm-spi", \
 com.ibm.wsspi.rest.handler.helper; type="ibm-spi"
-features=io.openliberty.restHandler.internal-1.0, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.securityAPI.javaee-1.0, \
  io.openliberty.securityAPI.jakarta-1.0
-jars=com.ibm.websphere.appserver.spi.restHandler; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.restHandler_2.0-javadoc.zip
kind=ga
edition=core
