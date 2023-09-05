-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenAPI-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
    uninstall
IBM-ShortName: mpOpenAPI-3.0
Subsystem-Name: MicroProfile OpenAPI 3.0
IBM-API-Package: \
    org.eclipse.microprofile.openapi.annotations;  type="stable",\
    org.eclipse.microprofile.openapi.annotations.callbacks; type="stable",\
    org.eclipse.microprofile.openapi.annotations.enums; type="stable",\
    org.eclipse.microprofile.openapi.annotations.extensions; type="stable",\
    org.eclipse.microprofile.openapi.annotations.headers; type="stable",\
    org.eclipse.microprofile.openapi.annotations.info; type="stable",\
    org.eclipse.microprofile.openapi.annotations.links; type="stable",\
    org.eclipse.microprofile.openapi.annotations.media; type="stable",\
    org.eclipse.microprofile.openapi.annotations.parameters; type="stable",\
    org.eclipse.microprofile.openapi.annotations.responses; type="stable",\
    org.eclipse.microprofile.openapi.annotations.security; type="stable",\
    org.eclipse.microprofile.openapi.annotations.servers; type="stable",\
    org.eclipse.microprofile.openapi.annotations.tags; type="stable",\
    org.eclipse.microprofile.openapi; type="stable",\
    org.eclipse.microprofile.openapi.models; type="stable",\
    org.eclipse.microprofile.openapi.models.callbacks; type="stable",\
    org.eclipse.microprofile.openapi.models.examples; type="stable",\
    org.eclipse.microprofile.openapi.models.headers; type="stable",\
    org.eclipse.microprofile.openapi.models.info; type="stable",\
    org.eclipse.microprofile.openapi.models.links; type="stable",\
    org.eclipse.microprofile.openapi.models.media; type="stable",\
    org.eclipse.microprofile.openapi.models.parameters; type="stable",\
    org.eclipse.microprofile.openapi.models.responses; type="stable",\
    org.eclipse.microprofile.openapi.models.security; type="stable",\
    org.eclipse.microprofile.openapi.models.servers; type="stable",\
    org.eclipse.microprofile.openapi.models.tags; type="stable",\
    org.eclipse.microprofile.openapi.spi; type="stable"
-features=io.openliberty.mpConfig-3.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.servlet.internal-5.0, \
  io.openliberty.restfulWS-3.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.org.eclipse.microprofile.openapi-3.0
-bundles=\
    io.openliberty.io.smallrye.openapi.core, \
    io.openliberty.io.smallrye.openapi.jaxrs, \
    io.openliberty.microprofile.openapi.2.0.internal.jakarta, \
    io.openliberty.microprofile.openapi.2.0.internal.servlet.jakarta, \
    io.openliberty.microprofile.openapi.2.0.internal.services, \
    com.ibm.ws.microprofile.openapi.ui, \
    io.openliberty.microprofile.openapi.internal.common,\
    io.openliberty.com.fasterxml.jackson, \
    com.ibm.ws.org.jboss.logging
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
