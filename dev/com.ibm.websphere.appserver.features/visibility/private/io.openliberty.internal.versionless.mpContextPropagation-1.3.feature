-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpContextPropagation-1.3
visibility=private
singleton=true
-features= \
  io.openliberty.internal.mpVersion-5.0; ibm.tolerates:="6.0,6.1,7.0,7.1", \
  io.openliberty.mpContextPropagation-1.3
kind=ga
edition=core
