-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restHandler1.0.internal.ee-9.0
singleton=true
visibility = private

IBM-SPI-Package: com.ibm.wsspi.rest.handler; type="ibm-spi", \
 com.ibm.wsspi.rest.handler.helper; type="ibm-spi"

-features=\
  io.openliberty.servlet.internal-5.0, \
  com.ibm.websphere.appserver.adminSecurity-2.0

-bundles= com.ibm.ws.rest.handler.jakarta

-jars=com.ibm.websphere.appserver.spi.restHandler; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.restHandler_2.0-javadoc.zip

kind=ga
edition=core

