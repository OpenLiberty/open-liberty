-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.webBundleSecurity.internal-1.0, \
  io.openliberty.servlet.internal-5.0, \
  io.openliberty.xmlws.common-3.0; apiJar=false, \
  com.ibm.websphere.appserver.globalhandler-2.0
-bundles=\
  com.ibm.ws.javaee.ddmodel.ws, \
  com.ibm.ws.webservices.javaee.common.jakarta, \
  com.ibm.ws.jaxws.2.3.common.jakarta; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.jaxws.webcontainer.jakarta, \
  com.ibm.ws.jaxws.web.jakarta, \
  com.ibm.ws.jaxws.wsat, \
  com.ibm.ws.wsat.common.jakarta; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.webclient.jakarta, \
  com.ibm.ws.wsat.webservice.jakarta, \
  io.openliberty.jaxws.globalhandler.internal.jakarta
kind=ga
edition=base
