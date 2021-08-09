-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs.common-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  javax.ws.rs; type="spec", \
  javax.ws.rs.container; type="spec", \
  javax.ws.rs.core; type="spec", \
  javax.ws.rs.client; type="spec", \
  javax.ws.rs.ext; type="spec", \
  com.ibm.websphere.jaxrs20.multipart; type="ibm-api", \
  com.ibm.websphere.jaxrs.providers.json4j; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-features=com.ibm.websphere.appserver.optional.jaxb-2.2; ibm.tolerates:="2.3", \
  com.ibm.websphere.appserver.json-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.servlet-3.1, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.javax.jaxrs-2.0, \
  com.ibm.websphere.appserver.globalhandler-1.0
-bundles=\
  com.ibm.websphere.javaee.jaxws.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.websphere.javaee.jws.1.0; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.ws.org.apache.xml.resolver.1.2, \
  com.ibm.ws.org.apache.neethi.3.0.2, \
  com.ibm.ws.jaxrs.2.0.common, \
  com.ibm.ws.jaxrs.2.x.config, \
  com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3
-jars=com.ibm.ws.jaxrs.2.0.tools
-files=\
  bin/jaxrs/wadl2java, \
  bin/jaxrs/wadl2java.bat, \
  bin/jaxrs/tools/wadl2java.jar
kind=ga
edition=core
WLP-Activation-Type: parallel
