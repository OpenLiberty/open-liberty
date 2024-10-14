-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpGraphQL-1.0
visibility=private
singleton=true
-features= \
  io.openliberty.internal.mpVersion-3.3; ibm.tolerates:="4.0,4.1", \
  com.ibm.websphere.appserver.mpGraphQL-1.0
kind=ga
edition=core
