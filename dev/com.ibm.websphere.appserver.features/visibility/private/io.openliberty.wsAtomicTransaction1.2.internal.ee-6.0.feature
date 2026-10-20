-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0
singleton=true
visibility = private
-features=\
  io.openliberty.webBundleSecurity.internal-1.0, \
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.httptransport-1.0, \
  io.openliberty.jaxws.common-2.2; apiJar=false, \
  com.ibm.websphere.appserver.globalhandler-1.0
-bundles=\
  com.ibm.ws.javaee.ddmodel.ws, \
  com.ibm.ws.webservices.javaee.common, \
  com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.jaxws.webcontainer, \
  com.ibm.ws.jaxws.web, \
  com.ibm.ws.jaxws.wsat, \
  com.ibm.ws.wsat.common; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.webclient, \
  com.ibm.ws.wsat.webservice, \
  io.openliberty.jaxws.globalhandler.internal
kind=ga
edition=base
