-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = com.ibm.wsspi.appserver.webBundle-1.0
visibility = protected

-bundles= \
 com.ibm.ws.app.manager.wab; start-phase:=APPLICATION_EARLY, \
 com.ibm.ws.eba.wab.integrator

-features= \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"

-jars= \
 com.ibm.websphere.appserver.spi.wab.configure; location:=dev/spi/ibm/

-files= \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wab.configure_1.0-javadoc.zip

IBM-SPI-Package: com.ibm.wsspi.wab.configure

Subsystem-Name: OSGi Application

edition=core
kind=ga
WLP-Activation-Type: parallel
