-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.opentracing-1.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: opentracing-1.1
Subsystem-Name: Opentracing 1.1
IBM-API-Package: io.opentracing;  type="third-party",\
                 io.opentracing.tag;  type="third-party",\
                 io.opentracing.propagation;  type="third-party", \
                 com.ibm.ws.opentracing.tracer; type="ibm-spi"
-features=com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:=2.1, \
          com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:=2.0
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.opentracing.1.1, \
         com.ibm.ws.opentracing.cdi, \
         com.ibm.websphere.appserver.thirdparty.opentracing; location:="dev/api/third-party/,lib/"
-jars=com.ibm.websphere.appserver.spi.opentracing; location:=dev/spi/ibm/
-files= dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.opentracing_1.1-javadoc.zip
kind=ga
edition=core
