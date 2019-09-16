-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsf-2.3
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.faces; type="spec", \
 javax.faces.annotation; type="spec", \
 javax.faces.application; type="spec", \
 javax.faces.bean; type="spec", \
 javax.faces.component; type="spec", \
 javax.faces.component.behavior; type="spec", \
 javax.faces.component.html; type="spec", \
 javax.faces.component.search; type="spec", \
 javax.faces.component.visit; type="spec", \
 javax.faces.context; type="spec", \
 javax.faces.convert; type="spec", \
 javax.faces.el; type="spec", \
 javax.faces.event; type="spec", \
 javax.faces.flow; type="spec", \
 javax.faces.flow.builder; type="spec", \
 javax.faces.lifecycle; type="spec", \
 javax.faces.model; type="spec", \
 javax.faces.push; type="spec", \
 javax.faces.render; type="spec", \
 javax.faces.validator; type="spec", \
 javax.faces.view; type="spec", \
 javax.faces.view.facelets; type="spec", \
 javax.faces.webapp; type="spec"
IBM-ShortName: jsf-2.3
Subsystem-Name: JavaServer Faces 2.3
-features=com.ibm.websphere.appserver.javax.cdi-2.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.javax.validation-2.0, \
 com.ibm.websphere.appserver.javax.jsf-2.3, \
 com.ibm.websphere.appserver.jsp-2.3, \
 com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
 com.ibm.websphere.appserver.jsfProvider-2.3.0.MyFaces, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=com.ibm.ws.org.apache.myfaces.2.3, \
 com.ibm.ws.org.apache.commons.beanutils.1.8.3, \
 com.ibm.ws.org.apache.commons.collections, \
 com.ibm.ws.org.apache.commons.discovery.0.2, \
 com.ibm.ws.org.apache.commons.logging.1.0.3, \
 com.ibm.ws.jsf.shared, \
 com.ibm.ws.cdi.interfaces, \
 com.ibm.ws.org.apache.commons.digester.1.8, \
 com.ibm.websphere.javaee.websocket.1.1; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.websocket:javax.websocket-api:1.1"
kind=ga
edition=core
