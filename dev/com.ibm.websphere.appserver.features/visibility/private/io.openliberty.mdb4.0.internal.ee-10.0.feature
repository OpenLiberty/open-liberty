-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.mdb4.0.internal.ee-10.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=base
