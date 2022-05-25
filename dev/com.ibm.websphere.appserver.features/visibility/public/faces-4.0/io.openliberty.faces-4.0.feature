-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.faces-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.faces; type="spec", \
 jakarta.faces.annotation; type="spec", \
 jakarta.faces.application; type="spec", \
 jakarta.faces.bean; type="spec", \
 jakarta.faces.component; type="spec", \
 jakarta.faces.component.behavior; type="spec", \
 jakarta.faces.component.html; type="spec", \
 jakarta.faces.component.search; type="spec", \
 jakarta.faces.component.visit; type="spec", \
 jakarta.faces.context; type="spec", \
 jakarta.faces.convert; type="spec", \
 jakarta.faces.el; type="spec", \
 jakarta.faces.event; type="spec", \
 jakarta.faces.flow; type="spec", \
 jakarta.faces.flow.builder; type="spec", \
 jakarta.faces.lifecycle; type="spec", \
 jakarta.faces.model; type="spec", \
 jakarta.faces.push; type="spec", \
 jakarta.faces.render; type="spec", \
 jakarta.faces.validator; type="spec", \
 jakarta.faces.view; type="spec", \
 jakarta.faces.view.facelets; type="spec", \
 jakarta.faces.webapp; type="spec", \
 org.apache.myfaces.renderkit.html; type="third-party", \
 org.apache.myfaces.shared.config; type="third-party", \
 org.apache.myfaces.shared.renderkit; type="third-party", \
 org.apache.myfaces.shared.renderkit.html; type="third-party", \
 org.apache.myfaces.shared.renderkit.html.util; type="third-party"
IBM-ShortName: faces-4.0
WLP-AlsoKnownAs: jsf-4.0
Subsystem-Name: Jakarta Server Faces 4.0
-features=io.openliberty.jakarta.websocket-2.1, \
  io.openliberty.servlet.api-6.0, \
  io.openliberty.jakarta.xmlBinding-4.0, \
  io.openliberty.facesProvider-3.0.0.MyFaces, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.validation-3.0, \
  io.openliberty.pages-3.1, \
  io.openliberty.jakarta.cdi-4.0, \
  io.openliberty.jakarta.faces-4.0
-bundles=io.openliberty.org.apache.myfaces.4.0, \
 com.ibm.ws.org.apache.commons.beanutils.1.9.4, \
 com.ibm.ws.org.apache.commons.collections, \
 com.ibm.ws.org.apache.commons.discovery.0.2, \
 io.openliberty.org.apache.commons.logging, \
 com.ibm.ws.jsf.shared.jakarta, \
 io.openliberty.faces.internal.jakarta, \
 com.ibm.ws.cdi.interfaces.jakarta, \
 com.ibm.ws.org.apache.commons.digester.1.8, \
 io.openliberty.jakarta.jstl.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0", \
 io.openliberty.faces.4.0.thirdparty; location:="dev/api/third-party/"
kind=noship
edition=full
WLP-Activation-Type: parallel
