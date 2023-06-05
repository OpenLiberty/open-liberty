-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundle.internal-1.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

Subsystem-Name: OSGi Application

-bundles= \
 com.ibm.ws.eba.wab.integrator

-features=io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.webBundle.internal.ee-6.0; ibm.tolerates:="9.0,10.0"

edition=core
kind=ga
WLP-Activation-Type: parallel
