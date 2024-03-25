-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.faces-4.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.faces; type="spec", \
 jakarta.faces.annotation; type="spec", \
 jakarta.faces.application; type="spec", \
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
 org.apache.myfaces.core.api.shared; type="spec", \
 org.apache.myfaces.core.api.shared.lang; type="spec", \
 org.apache.myfaces.renderkit.html; type="third-party", \
 org.apache.myfaces.config.webparameters; type="third-party", \
 org.apache.myfaces.renderkit; type="third-party", \
 org.apache.myfaces.renderkit.html.base; type="third-party", \
 org.apache.myfaces.renderkit.html.util; type="third-party"
IBM-ShortName: faces-4.1
WLP-AlsoKnownAs: jsf-4.1
Subsystem-Name: Jakarta Faces 4.1
-features=io.openliberty.jakarta.websocket-2.2, \
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.facesProvider-4.1.0.MyFaces, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jakarta.validation-3.1, \
  io.openliberty.jakarta.faces-4.1, \
  io.openliberty.expressionLanguage-6.0, \
  io.openliberty.cdi-4.1
-bundles=io.openliberty.org.apache.myfaces.4.1, \
 com.ibm.ws.jsf.shared.jakarta, \
 io.openliberty.faces.4.0.internal, \
 com.ibm.ws.cdi.interfaces.jakarta, \
 com.ibm.ws.cdi.2.0.jsf.jakarta, \
 io.openliberty.faces.4.1.thirdparty; location:="dev/api/third-party/"
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
