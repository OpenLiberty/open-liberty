-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-3.0
Subsystem-Name: MicroProfile OpenTracing 3.0
IBM-API-Package: \
    org.eclipse.microprofile.opentracing; type="stable", \
    io.opentracing;  type="third-party", \
    io.opentracing.tag;  type="third-party", \
    io.opentracing.propagation;  type="third-party"
IBM-SPI-Package: \
    io.openliberty.opentracing.spi.tracer
-features= \
    io.openliberty.mpConfig-3.0, \
    io.openliberty.restfulWS-3.0, \
    io.openliberty.cdi-3.0, \
    io.openliberty.org.eclipse.microprofile.opentracing-3.0, \
    io.openliberty.mpCompatible-5.0
-bundles= \
    io.openliberty.microprofile.opentracing.2.0.internal.jakarta; apiJar=false; location:="lib/", \
    com.ibm.ws.jaxrs.defaultexceptionmapper.jakarta, \
    io.openliberty.opentracing.2.0.internal.jakarta, \
    io.openliberty.opentracing.2.0.internal.cdi.jakarta, \
    io.openliberty.io.opentracing.opentracing-util.0.33.0, \
    io.openliberty.opentracing.2.0.thirdparty; location:="dev/api/third-party/,lib/"; mavenCoordinates="io.opentracing:opentracing-api:0.33.0", \
    com.ibm.ws.microprofile.opentracing.jaeger, \
    com.ibm.ws.microprofile.opentracing.jaeger.adapter, \
    com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl,\
    io.openliberty.opentracing.3.0.internal.restfulws, \
    io.openliberty.opentracing.2.0.spi; location:="dev/spi/ibm/,lib/", \
    io.openliberty.microprofile.opentracing.common
-files= \
    dev/spi/ibm/javadoc/io.openliberty.opentracing.2.0.spi_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
