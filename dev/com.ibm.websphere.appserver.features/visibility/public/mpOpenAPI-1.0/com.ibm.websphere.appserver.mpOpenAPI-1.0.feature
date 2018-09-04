-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenAPI-1.0
visibility=public
singleton=true
IBM-ShortName: mpOpenAPI-1.0
Subsystem-Name: MicroProfile OpenAPI 1.0
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
 com.ibm.websphere.appserver.org.eclipse.microprofile.openapi-1.0, \
 com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:=4.0, \
 com.ibm.websphere.appserver.mpConfig-1.2; ibm.tolerates:=1.3, \
 com.ibm.websphere.appserver.internal.optional.jaxb-2.2,\
 com.ibm.wsspi.appserver.webBundle-1.0,\
 com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:=2.1
-bundles=\
 com.ibm.ws.require.java8, \
 com.ibm.ws.microprofile.openapi,\
 com.ibm.ws.microprofile.openapi.ui,\
 com.ibm.ws.com.fasterxml.jackson.2.9.1
kind=ga
edition=core
