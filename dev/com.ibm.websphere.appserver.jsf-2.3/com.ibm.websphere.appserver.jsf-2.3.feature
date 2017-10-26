-include= ~../cnf/resources/bnd/feature.props
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
 com.ibm.websphere.appserver.javax.validation-1.1, \
 com.ibm.websphere.appserver.javax.jsf-2.3, \
 com.ibm.websphere.appserver.jsp-2.3, \
 com.ibm.websphere.appserver.javaeeCompatible-7.0
-bundles=com.ibm.ws.org.apache.myfaces.2.3, \
 com.ibm.ws.org.apache.commons.beanutils.1.8.3, \
 com.ibm.ws.org.apache.commons.collections.3.2.1, \
 com.ibm.ws.org.apache.commons.discovery.0.2, \
 com.ibm.ws.org.apache.commons.logging.1.0.3, \
 com.ibm.ws.jsf.shared, \
 com.ibm.ws.cdi.interfaces, \
 com.ibm.ws.org.apache.commons.digester.1.8, \
 com.ibm.websphere.javaee.websocket.1.1; location:="dev/api/spec/,lib/"
kind=beta
edition=core
