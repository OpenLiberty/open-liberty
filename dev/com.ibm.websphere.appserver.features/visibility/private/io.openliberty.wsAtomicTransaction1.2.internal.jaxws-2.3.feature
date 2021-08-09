-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.3
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.jaxws-2.3
-bundles=\
  com.ibm.ws.wsat.common.3.2; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.cxf.utils.3.2, \
  com.ibm.ws.wsat.webclient.3.2, \
  com.ibm.ws.wsat.webservice.3.2
kind=noship
edition=full
