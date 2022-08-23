-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-bundles= \
 com.ibm.ws.eba.wab.integrator

-features=io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  io.openliberty.webBundle.internal.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0"

Subsystem-Name: OSGi Application

edition=core
kind=ga
WLP-Activation-Type: parallel
