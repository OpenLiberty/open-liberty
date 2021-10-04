-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.opentracing-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: opentracing-2.0
Subsystem-Name: Opentracing 2.0
IBM-API-Package: io.opentracing;  type="third-party",\
                 io.opentracing.tag;  type="third-party",\
                 io.opentracing.propagation;  type="third-party", \
                 io.openliberty.opentracing.spi.tracer; type="ibm-spi"
-features=com.ibm.websphere.appserver.mpConfig-2.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  com.ibm.websphere.appserver.cdi-2.0
-bundles=com.ibm.ws.jaxrs.defaultexceptionmapper, \
         com.ibm.ws.jaxrs.2.x.defaultexceptionmapper, \
         io.openliberty.opentracing.2.0.internal, \
         io.openliberty.opentracing.2.0.internal.cdi, \
         io.openliberty.io.opentracing.opentracing-util.0.33.0, \
         io.openliberty.opentracing.2.0.thirdparty; location:="dev/api/third-party/,lib/"; mavenCoordinates="io.opentracing:opentracing-api:0.33.0", \
         com.ibm.ws.microprofile.opentracing.jaeger, \
         com.ibm.ws.microprofile.opentracing.jaeger.adapter, \
         com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl
-jars=io.openliberty.opentracing.2.0.spi; location:=dev/spi/ibm/
-files= dev/spi/ibm/javadoc/io.openliberty.opentracing.2.0.spi_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
