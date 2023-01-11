-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.data-1.0
visibility=private
singleton=true
#TODO com.ibm.websphere.appserver.eeCompatible-11.0
-features=\
  io.openliberty.jakarta.annotation-2.1,\
  io.openliberty.jakarta.cdi-4.0,\
  io.openliberty.jakarta.interceptor-2.1,\
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.data.1.0; location:="dev/api/spec/,lib/"
kind=beta
edition=base
WLP-Activation-Type: parallel
