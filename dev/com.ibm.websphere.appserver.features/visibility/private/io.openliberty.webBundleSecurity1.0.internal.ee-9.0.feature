-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundleSecurity1.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  io.openliberty.servlet.internal-5.0, \
  io.openliberty.securityAPI.jakarta-1.0

-bundles= io.openliberty.webcontainer.security.internal; start-phase:=SERVICE_EARLY, \
          io.openliberty.security.authentication.internal.filter, \
          io.openliberty.security.authentication.internal.tai, \
          io.openliberty.security.sso.internal

edition=core
kind=ga
WLP-Activation-Type: parallel
