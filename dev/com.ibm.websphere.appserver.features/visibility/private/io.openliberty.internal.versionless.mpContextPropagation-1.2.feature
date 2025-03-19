-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpContextPropagation-1.2
visibility=private
singleton=true
-features= \
  io.openliberty.internal.mpVersion-4.0; ibm.tolerates:="4.1", \
  com.ibm.websphere.appserver.mpContextPropagation-1.2
kind=ga
edition=core
