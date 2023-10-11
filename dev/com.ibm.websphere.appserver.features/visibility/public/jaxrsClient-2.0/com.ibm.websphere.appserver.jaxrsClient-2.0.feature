-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsClient-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  com.ibm.websphere.jaxrs20.multipart; type="ibm-api", \
  com.ibm.websphere.jaxrs.providers.json4j; type="ibm-api", \
  javax.ws.rs; type="spec", \
  javax.ws.rs.container; type="spec", \
  javax.ws.rs.core; type="spec", \
  javax.ws.rs.client; type="spec", \
  javax.ws.rs.ext; type="spec", \
  javax.activation; type="spec"; require-java:="9", \
  javax.xml.bind; type="spec"; require-java:="9", \
  javax.xml.bind.annotation; type="spec"; require-java:="9", \
  javax.xml.bind.annotation.adapters; type="spec"; require-java:="9", \
  javax.xml.bind.attachment; type="spec"; require-java:="9", \
  javax.xml.bind.helpers; type="spec"; require-java:="9", \
  javax.xml.bind.util; type="spec"; require-java:="9"
IBM-SPI-Package: \
  com.ibm.wsspi.webservices.handler
IBM-ShortName: jaxrsClient-2.0
Subsystem-Name: Java RESTful Services Client 2.0
-features=com.ibm.websphere.appserver.jaxrs.common-2.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=com.ibm.ws.jaxrs.2.0.client
kind=ga
edition=core
WLP-Activation-Type: parallel
