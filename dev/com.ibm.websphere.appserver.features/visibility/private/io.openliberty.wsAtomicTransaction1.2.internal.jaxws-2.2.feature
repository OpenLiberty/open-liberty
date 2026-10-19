-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.2
singleton=true
visibility = private
WLP-DisableAllFeatures-OnConflict: false
# Registers javax.xml.soap as a core-exported package on Java 9+ so the kernel
# system bundle shim export is blocked and the SAAJ API from jws.1.0 is wired instead.
# third-party is not in the default app API types, so applications do not see it.
IBM-API-Package: \
  javax.xml.soap; type="third-party"; require-java:="9"
-features=\
  io.openliberty.webBundleSecurity.internal-1.0, \
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.httptransport-1.0, \
  com.ibm.websphere.appserver.internal.cxf.common-3.2, \
  com.ibm.websphere.appserver.optional.jaxb-2.2; apiJar=false, \
  com.ibm.websphere.appserver.internal.optional.jaxws-2.2, \
  com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:="1.6", \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  com.ibm.websphere.appserver.globalhandler-1.0
-bundles=\
  com.ibm.ws.javaee.ddmodel.ws, \
  com.ibm.ws.javaee.ddmodel.wsbnd, \
  com.ibm.ws.webservices.javaee.common, \
  com.ibm.websphere.javaee.jws.1.0; require-java:="9"; apiJar=false; location:="dev/api/spec/,lib/", \
  com.ibm.websphere.javaee.wsdl4j.1.2; apiJar=false; location:="dev/api/spec/,lib/", \
  com.ibm.websphere.prereq.wsdl4j.api; apiJar=false; location:="dev/api/spec/,lib/", \
  com.ibm.ws.prereq.wsdl4j.1.6.2, \
  com.ibm.ws.com.sun.xml.messaging.saaj; require-java:="9", \
  com.ibm.ws.org.jvnet.mimepull; require-java:="9", \
  com.ibm.ws.jaxws.tools.2.2.10, \
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.features.logging.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2, \
  io.openliberty.jaxws.globalhandler.internal, \
  com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.jaxws.webcontainer, \
  com.ibm.ws.jaxws.web, \
  com.ibm.ws.jaxws.wsat, \
  com.ibm.ws.wsat.common; start-phase:=CONTAINER_LATE, \
  com.ibm.ws.wsat.webclient, \
  com.ibm.ws.wsat.webservice
kind=ga
edition=base
