-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcpServer1.0.ee-11.0
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.cdi-4.1, \
  io.openliberty.jsonb-3.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0,\
  io.openliberty.noShip-1.0
kind=noship
edition=full
WLP-Activation-Type: parallel
