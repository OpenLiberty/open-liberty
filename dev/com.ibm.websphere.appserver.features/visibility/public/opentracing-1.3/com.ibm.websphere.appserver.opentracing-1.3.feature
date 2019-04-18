-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.opentracing-1.3
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: opentracing-1.3
Subsystem-Name: Opentracing 1.3
IBM-API-Package: io.opentracing;  type="third-party",\
                 io.opentracing.tag;  type="third-party",\
                 io.opentracing.propagation;  type="third-party", \
                 com.ibm.ws.opentracing.tracer; type="ibm-spi"
-features=com.ibm.websphere.appserver.jaxrs-2.1, \
          com.ibm.websphere.appserver.cdi-2.0, \
          com.ibm.websphere.appserver.mpConfig-1.3
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.opentracing.1.3, \
         com.ibm.ws.opentracing.1.3.cdi, \
         com.ibm.websphere.appserver.thirdparty.opentracing.0.31.0; location:="dev/api/third-party/,lib/"; mavenCoordinates="io.opentracing:opentracing-api:0.31.0"
-jars=com.ibm.websphere.appserver.spi.opentracing.1.3; location:=dev/spi/ibm/
-files= dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.opentracing.1.3_1.0-javadoc.zip
kind=ga
edition=core
