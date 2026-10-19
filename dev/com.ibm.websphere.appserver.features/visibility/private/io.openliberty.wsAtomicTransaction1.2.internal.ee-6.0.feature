-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0
singleton=true
visibility = private
WLP-DisableAllFeatures-OnConflict: false
-features=\
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0", \
  io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.2
kind=ga
edition=base
