-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrs.common-2.1
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
-features=com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.6, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.javax.annotation-1.3; apiJar=false, \
 com.ibm.websphere.appserver.json-1.0, \
 com.ibm.websphere.appserver.jsonpInternal-1.1, \
 com.ibm.websphere.appserver.jsonbInternal-1.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=com.ibm.websphere.appserver.api.jaxrs20; location:="dev/api/ibm/,lib/", \
 com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.jaxrs.2.1.common, \
 com.ibm.ws.jaxrs.2.x.config, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.websphere.javaee.jaxrs.2.1; location:="dev/api/spec/,lib/", \
 com.ibm.ws.jaxrs.2.0.tools
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jaxrs20_1.0-javadoc.zip, \
 bin/jaxrs/wadl2java, \
 bin/jaxrs/wadl2java.bat, \
 bin/jaxrs/tools/wadl2java.jar
kind=beta
edition=core
