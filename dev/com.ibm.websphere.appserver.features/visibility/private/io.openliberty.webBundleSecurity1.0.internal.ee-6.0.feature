-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundleSecurity1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0"

-bundles= com.ibm.ws.webcontainer.security; start-phase:=SERVICE_EARLY, \
		  com.ibm.ws.security.authentication.filter, \
          com.ibm.ws.security.authentication.tai, \
          com.ibm.ws.security.sso

edition=core
kind=ga
WLP-Activation-Type: parallel
