-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-10.0
singleton=true
visibility = private
-features=\
  io.openliberty.xmlWS-4.0, \
  com.ibm.websphere.appserver.servlet-6.0
-bundles=\
  com.ibm.ws.wsat.common.jakarta; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.webclient.jakarta, \
  com.ibm.ws.wsat.webservice.jakarta
kind=beta
edition=base
