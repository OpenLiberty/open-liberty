-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenAPI-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
    uninstall
IBM-ShortName: mpOpenAPI-2.0
Subsystem-Name: MicroProfile OpenAPI 2.0
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
    org.eclipse.microprofile.openapi.models.tags; type="stable"
IBM-SPI-Package: \
    org.eclipse.microprofile.openapi.spi; type="stable"
-features=\
    io.openliberty.org.eclipse.microprofile.openapi-2.0, \
    com.ibm.websphere.appserver.mpConfig-1.3; ibm.tolerates:="1.4", \
    com.ibm.websphere.appserver.servlet-4.0, \
    com.ibm.wsspi.appserver.webBundle-1.0, \
    com.ibm.websphere.appserver.jaxrs-2.1
-bundles=\
    io.openliberty.io.smallrye.openapi.core, \
    io.openliberty.io.smallrye.openapi.jaxrs, \
    io.openliberty.microprofile.openapi.2.0.internal, \
    com.ibm.ws.microprofile.openapi.ui, \
    com.ibm.ws.com.fasterxml.jackson.2.9.1, \
    com.ibm.ws.org.jboss.logging
kind=beta
edition=core
WLP-Activation-Type: parallel
