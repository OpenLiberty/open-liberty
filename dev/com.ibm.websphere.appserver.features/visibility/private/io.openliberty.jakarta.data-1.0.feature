-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.data-1.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.jakarta.annotation-2.1; ibm.tolerates:="3.0",\
  io.openliberty.jakarta.cdi-4.0; ibm.tolerates:="4.1"
-bundles=\
  io.openliberty.jakarta.data.1.0; location:="dev/api/spec/,lib/"
kind=beta
edition=base
WLP-Activation-Type: parallel
