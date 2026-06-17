-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging3.1.ee-10.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=base