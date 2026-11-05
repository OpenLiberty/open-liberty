-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxws.common-2.2
visibility=private
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Internal JAX-WS 2.2 Common Feature for jaxws-2.2, jaxwsClient-2.2 server and client features
IBM-API-Package: \
  javax.xml.soap; type="spec"; require-java:="9"
-features=\
  com.ibm.websphere.appserver.jaxb-2.2, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:="1.6", \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  com.ibm.websphere.appserver.internal.cxf.common-3.2, \
  com.ibm.websphere.appserver.internal.optional.jaxws-2.2
-bundles=\
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.features.logging.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2, \
  io.openliberty.org.apache.neethi, \
  com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
  com.ibm.ws.prereq.wsdl4j.1.6.2, \
  com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
  com.ibm.ws.com.sun.xml.messaging.saaj; require-java:="9", \
  com.ibm.ws.org.jvnet.mimepull; require-java:="9", \
  com.ibm.websphere.javaee.jws.1.0; require-java:="9"; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.jws:jsr181-api:1.0-MR1", \
  com.ibm.websphere.javaee.jaxws.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.2.12", \
  com.ibm.ws.jaxws.tools.2.2.10, \
  com.ibm.ws.javaee.ddmodel.wsbnd
kind=ga
edition=base
WLP-Activation-Type: parallel
