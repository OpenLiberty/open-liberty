-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
WLP-Activation-Type: parallel
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxws-2.2
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
IBM-API-Package: \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal", \
 org.apache.cxf.databinding;type="internal"
Subsystem-Name: Java Web Services 2.2
-features=\
  io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.jaxws.common-2.2, \
  com.ibm.websphere.appserver.globalhandler-1.0
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.2.3.wsat, \
 com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
 com.ibm.ws.webservices.javaee.common
kind=ga
edition=base
