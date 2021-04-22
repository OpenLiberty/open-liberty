-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.audit1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-features=\
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"

-bundles=\
  com.ibm.ws.security.audit.file, \
  com.ibm.ws.request.probe.audit.servlet

kind=ga
edition=core
