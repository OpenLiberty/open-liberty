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
 javax.faces.webapp; type="spec", \
 org.apache.myfaces.application; type="third-party", \
 org.apache.myfaces.application.jsp; type="third-party", \
 org.apache.myfaces.application.viewstate; type="third-party", \
 org.apache.myfaces.application.viewstate.token; type="third-party", \
 org.apache.myfaces.cdi; type="third-party", \
 org.apache.myfaces.cdi.behavior; type="third-party", \
 org.apache.myfaces.cdi.config; type="third-party", \
 org.apache.myfaces.cdi.converter; type="third-party", \
 org.apache.myfaces.cdi.impl; type="third-party", \
 org.apache.myfaces.cdi.managedproperty; type="third-party", \
 org.apache.myfaces.cdi.model; type="third-party", \
 org.apache.myfaces.cdi.scope; type="third-party", \
 org.apache.myfaces.cdi.util; type="third-party", \
 org.apache.myfaces.cdi.validator; type="third-party", \
 org.apache.myfaces.cdi.view; type="third-party", \
 org.apache.myfaces.component; type="third-party", \
 org.apache.myfaces.component.search; type="third-party", \
 org.apache.myfaces.component.validate; type="third-party", \
 org.apache.myfaces.component.visit; type="third-party", \
 org.apache.myfaces.config; type="third-party", \
 org.apache.myfaces.config.annotation; type="third-party", \
 org.apache.myfaces.config.element; type="third-party", \
 org.apache.myfaces.config.element.facelets; type="third-party", \
 org.apache.myfaces.config.impl; type="third-party", \
 org.apache.myfaces.config.impl.digester; type="third-party", \
 org.apache.myfaces.config.impl.digester.elements; type="third-party", \
 org.apache.myfaces.config.impl.digester.elements.facelets; type="third-party", \
 org.apache.myfaces.config.util; type="third-party", \
 org.apache.myfaces.context; type="third-party", \
 org.apache.myfaces.context.servlet; type="third-party", \
 org.apache.myfaces.convert; type="third-party", \
 org.apache.myfaces.ee; type="third-party", \
 org.apache.myfaces.el; type="third-party", \
 org.apache.myfaces.el.convert; type="third-party", \
 org.apache.myfaces.el.unified; type="third-party", \
 org.apache.myfaces.el.unified.resolver; type="third-party", \
 org.apache.myfaces.el.unified.resolver.implicitobject; type="third-party", \
 org.apache.myfaces.event; type="third-party", \
 org.apache.myfaces.flow; type="third-party", \
 org.apache.myfaces.flow.builder; type="third-party", \
 org.apache.myfaces.flow.cdi; type="third-party", \
 org.apache.myfaces.flow.impl; type="third-party", \
 org.apache.myfaces.flow.util; type="third-party", \
 org.apache.myfaces.lifecycle; type="third-party", \
 org.apache.myfaces.push; type="third-party", \
 org.apache.myfaces.push.cdi; type="third-party", \
 org.apache.myfaces.push.util; type="third-party", \
 org.apache.myfaces.renderkit; type="third-party", \
 org.apache.myfaces.renderkit.html; type="third-party", \
 org.apache.myfaces.resource; type="third-party", \
 org.apache.myfaces.shared; type="third-party", \
 org.apache.myfaces.shared.application; type="third-party", \
 org.apache.myfaces.shared.component; type="third-party", \
 org.apache.myfaces.shared.config; type="third-party", \
 org.apache.myfaces.shared.context; type="third-party", \
 org.apache.myfaces.shared.context.flash; type="third-party", \
 org.apache.myfaces.shared.el; type="third-party", \
 org.apache.myfaces.shared.renderkit; type="third-party", \
 org.apache.myfaces.shared.renderkit.html; type="third-party", \
 org.apache.myfaces.shared.renderkit.html.util; type="third-party", \
 org.apache.myfaces.shared.resource; type="third-party", \
 org.apache.myfaces.shared.taglib; type="third-party", \
 org.apache.myfaces.shared.taglib.core; type="third-party", \
 org.apache.myfaces.shared.test; type="third-party", \
 org.apache.myfaces.shared.trace; type="third-party", \
 org.apache.myfaces.shared.util; type="third-party", \
 org.apache.myfaces.shared.util.el; type="third-party", \
 org.apache.myfaces.shared.util.io; type="third-party", \
 org.apache.myfaces.shared.util.renderkit; type="third-party", \
 org.apache.myfaces.shared.util.serial; type="third-party", \
 org.apache.myfaces.shared.util.servlet; type="third-party", \
 org.apache.myfaces.shared.util.xml; type="third-party", \
 org.apache.myfaces.shared.view; type="third-party", \
 org.apache.myfaces.shared.webapp.webxml; type="third-party", \
 org.apache.myfaces.shared_impl.util.serial; type="third-party", \
 org.apache.myfaces.shared_impl.webapp.webxml; type="third-party", \
 org.apache.myfaces.spi; type="third-party", \
 org.apache.myfaces.spi.impl; type="third-party", \
 org.apache.myfaces.taglib.core; type="third-party", \
 org.apache.myfaces.taglib.html; type="third-party", \
 org.apache.myfaces.util; type="third-party", \
 org.apache.myfaces.view; type="third-party", \
 org.apache.myfaces.view.facelets; type="third-party", \
 org.apache.myfaces.view.facelets.compiler; type="third-party", \
 org.apache.myfaces.view.facelets.component; type="third-party", \
 org.apache.myfaces.view.facelets.el; type="third-party", \
 org.apache.myfaces.view.facelets.impl; type="third-party", \
 org.apache.myfaces.view.facelets.pool; type="third-party", \
 org.apache.myfaces.view.facelets.pool.impl; type="third-party", \
 org.apache.myfaces.view.facelets.tag; type="third-party", \
 org.apache.myfaces.view.facelets.tag.composite; type="third-party", \
 org.apache.myfaces.view.facelets.tag.jsf; type="third-party", \
 org.apache.myfaces.view.facelets.tag.jsf.core; type="third-party", \
 org.apache.myfaces.view.facelets.tag.jsf.html; type="third-party", \
 org.apache.myfaces.view.facelets.tag.jstl.core; type="third-party", \
 org.apache.myfaces.view.facelets.tag.jstl.fn; type="third-party", \
 org.apache.myfaces.view.facelets.tag.ui; type="third-party", \
 org.apache.myfaces.view.facelets.util; type="third-party", \
 org.apache.myfaces.view.impl; type="third-party", \
 org.apache.myfaces.view.jsp; type="third-party", \
 org.apache.myfaces.webapp; type="third-party"
IBM-ShortName: jsf-2.3
Subsystem-Name: JavaServer Faces 2.3
-features=com.ibm.websphere.appserver.javax.cdi-2.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.javax.validation-2.0, \
 com.ibm.websphere.appserver.javax.jsf-2.3, \
 com.ibm.websphere.appserver.jsp-2.3, \
 com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
 com.ibm.websphere.appserver.jsfApiStub-2.3, \
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
 com.ibm.websphere.javaee.websocket.1.1; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.websocket:javax.websocket-api:1.1", \
 com.ibm.websphere.appserver.thirdparty.jsf-2.3; location:="dev/api/third-party/"; mavenCoordinates="org.apache.myfaces.core:myfaces-impl:2.3.3"
kind=ga
edition=core
