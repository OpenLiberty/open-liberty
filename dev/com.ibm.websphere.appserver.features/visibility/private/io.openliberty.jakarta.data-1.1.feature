-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.data-1.1
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0,\
  io.openliberty.jakarta.annotation-3.0,\
  io.openliberty.jakarta.cdi-4.1
-bundles=\
  io.openliberty.jakarta.data.1.1; location:="dev/api/spec/,lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel
