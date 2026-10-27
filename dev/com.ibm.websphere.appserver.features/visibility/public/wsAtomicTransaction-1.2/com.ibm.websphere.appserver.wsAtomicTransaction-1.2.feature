-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsAtomicTransaction-1.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: wsAtomicTransaction-1.2
IBM-API-Package: com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.webservices.wsat, \
  com.ibm.wsspi.anno.classsource, \
  com.ibm.wsspi.anno.info, \
  com.ibm.wsspi.anno.service, \
  com.ibm.wsspi.anno.targets, \
  com.ibm.wsspi.anno.util, \
  com.ibm.ws.anno.classsource.specification, \
  com.ibm.wsspi.artifact, \
  com.ibm.wsspi.artifact.equinox.module, \
  com.ibm.wsspi.artifact.factory, \
  com.ibm.wsspi.artifact.factory.contributor, \
  com.ibm.wsspi.artifact.overlay, \
  com.ibm.wsspi.adaptable.module, \
  com.ibm.wsspi.adaptable.module.adapters, \
  com.ibm.ws.adaptable.module.structure
Manifest-Version: 1.0
Subsystem-Name: WS-AT Service 1.2
-features=com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1, 2.0, 2.1", \
  io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.2; ibm.tolerates:="3.0,4.0", \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1,6.2", \
  io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0; ibm.tolerates:="9.0, 10.0"
-jars=com.ibm.websphere.appserver.spi.wsat; location:=dev/spi/ibm/, \
  io.openliberty.wsat.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wsat_1.0-javadoc.zip, \
  dev/spi/ibm/javadoc/io.openliberty.wsat.spi_1.0-javadoc.zip
kind=ga
edition=base
