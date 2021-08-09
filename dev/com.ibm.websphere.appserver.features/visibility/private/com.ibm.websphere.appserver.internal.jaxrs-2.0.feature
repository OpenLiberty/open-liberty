-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jaxrs-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Java RESTful Services 2.0
-features=com.ibm.websphere.appserver.json-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.servlet-3.1, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0, \
  com.ibm.websphere.appserver.javax.jaxrs-2.0, \
  com.ibm.websphere.appserver.globalhandler-1.0
-bundles=com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.jaxrs.2.0.common, \
 com.ibm.ws.jaxrs.2.x.config, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.jaxrs.2.0.web, \
 com.ibm.ws.jaxrs.2.0.server, \
 com.ibm.ws.jaxrs.2.0.client
-jars=com.ibm.ws.jaxrs.2.0.tools
-files=bin/jaxrs/wadl2java, \
 bin/jaxrs/wadl2java.bat, \
 bin/jaxrs/tools/wadl2java.jar
kind=ga
edition=core
WLP-Activation-Type: parallel
