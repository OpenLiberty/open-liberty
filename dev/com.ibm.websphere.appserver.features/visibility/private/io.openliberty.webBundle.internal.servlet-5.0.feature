-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.servlet-5.0
singleton=true
visibility = private

Subsystem-Name: OSGi Application

-features=io.openliberty.servlet.api-5.0, \
  com.ibm.websphere.appserver.servlet-5.0

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY, \
  com.ibm.ws.eba.wab.integrator

-jars= \
 com.ibm.websphere.appserver.spi.wab.configure; location:=dev/spi/ibm/

-files= \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wab.configure_1.0-javadoc.zip

IBM-SPI-Package: com.ibm.wsspi.wab.configure

kind=ga
edition=core
WLP-Activation-Type: parallel
