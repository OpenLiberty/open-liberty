-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.mdb4.0.internal.ee-12.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.connectors-2.2, \
  com.ibm.websphere.appserver.transaction-2.1
kind=noship
edition=full
