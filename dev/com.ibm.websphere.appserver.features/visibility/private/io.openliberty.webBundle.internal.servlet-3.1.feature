-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.servlet-3.1
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

Subsystem-Name: OSGi Application

-features=io.openliberty.servlet.api-3.1, \
  com.ibm.websphere.appserver.servlet-3.1

-bundles= com.ibm.ws.app.manager.wab; start-phase:=APPLICATION_EARLY, \
  com.ibm.ws.eba.wab.integrator

-jars= \
 com.ibm.websphere.appserver.spi.wab.configure; location:=dev/spi/ibm/

-files= \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wab.configure_1.0-javadoc.zip

IBM-SPI-Package: com.ibm.wsspi.wab.configure

edition=core
kind=ga
WLP-Activation-Type: parallel
