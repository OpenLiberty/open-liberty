-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpGraphQL-2.0
visibility=private
singleton=true
-features= \
  io.openliberty.internal.mpVersion-5.0; ibm.tolerates:="6.0,6.1,7.0", \
  io.openliberty.mpGraphQL-2.0
kind=ga
edition=core
