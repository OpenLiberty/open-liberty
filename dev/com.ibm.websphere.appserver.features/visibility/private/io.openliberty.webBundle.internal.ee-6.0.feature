-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-features=io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1, 4.0"

-bundles= com.ibm.ws.app.manager.wab; start-phase:=APPLICATION_EARLY

-jars= \
 com.ibm.websphere.appserver.spi.wab.configure; location:=dev/spi/ibm/

-files= \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.wab.configure_1.0-javadoc.zip

IBM-SPI-Package: com.ibm.wsspi.wab.configure

edition=core
kind=ga
WLP-Activation-Type: parallel
