-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs.common-2.1
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
-features=\
  com.ibm.websphere.appserver.javax.jaxrs-2.1, \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.javax.mail-1.6, \
  com.ibm.websphere.appserver.globalhandler-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.jsonpInternal-1.1, \
  com.ibm.websphere.appserver.jsonbInternal-1.0, \
  com.ibm.websphere.appserver.optional.jaxb-2.2,\
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.internal.cxf.common-3.2
-bundles=\
  com.ibm.websphere.appserver.api.jaxrs20; location:="dev/api/ibm/,lib/", \
  com.ibm.websphere.javaee.jaxws.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.websphere.javaee.jws.1.0; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.ws.jaxrs.2.1.common, \
  com.ibm.ws.jaxrs.2.x.config, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxrs.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.rs.client.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.rs.service.description.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.rs.sse.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.hc.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.wadlto.jaxrs.3.2, \
  com.ibm.ws.jaxrs.2.0.tools
-files=\
  bin/jaxrs/wadl2java, \
  bin/jaxrs/wadl2java.bat, \
  bin/jaxrs/tools/wadl2java.jar
kind=ga
edition=core
