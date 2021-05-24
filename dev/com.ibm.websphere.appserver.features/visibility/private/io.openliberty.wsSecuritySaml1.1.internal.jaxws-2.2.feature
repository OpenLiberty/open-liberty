-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.wsSecuritySaml1.1.internal.jaxws-2.2
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\ 
  com.ibm.websphere.appserver.jaxws-2.2, \
  io.openliberty.samlWeb2.0.internal.opensaml-2.6
kind=ga
edition=base
