-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsAtomicTransaction-1.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: wsAtomicTransaction-1.2
IBM-SPI-Package: com.ibm.wsspi.webservices.wsat
Manifest-Version: 1.0
Subsystem-Name: WS-AT Service 1.2
-features=com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1", \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.jaxws-2.2, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
-bundles=com.ibm.ws.wsat.common; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.webclient, \
  com.ibm.ws.wsat.webservice, \
  com.ibm.ws.jaxws.wsat
-jars=com.ibm.websphere.appserver.spi.wsat; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wsat_1.0-javadoc.zip
kind=ga
edition=base
