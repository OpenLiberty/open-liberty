-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.xmlWS-3.0, \
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  com.ibm.ws.wsat.common.3.2.jakarta; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.cxf.utils.3.2.jakarta, \
  com.ibm.ws.wsat.webclient.3.2.jakarta, \
  com.ibm.ws.wsat.webservice.3.2.jakarta
-jars=io.openliberty.wsat.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/io.openliberty.wsat.spi_1.0-javadoc.zip
kind=beta
edition=base
