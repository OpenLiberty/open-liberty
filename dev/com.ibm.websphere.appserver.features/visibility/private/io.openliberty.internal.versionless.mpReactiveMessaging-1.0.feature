-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpReactiveMessaging-1.0
visibility=private
singleton=true
-features= \
  io.openliberty.internal.mpVersion-1.4; ibm.tolerates:="2.0,2.1,2.2,3.0,3.2,3.3", \
  com.ibm.websphere.appserver.mpReactiveMessaging-1.0
kind=ga
edition=core
