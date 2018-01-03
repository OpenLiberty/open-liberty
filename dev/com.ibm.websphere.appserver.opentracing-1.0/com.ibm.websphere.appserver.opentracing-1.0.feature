-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.opentracing-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: opentracing-1.0
Subsystem-Name: Opentracing 1.0

IBM-API-Package: io.opentracing;  type="third-party",\
                 io.opentracing.tag;  type="third-party",\
                 io.opentracing.propagation;  type="third-party", \
                 com.ibm.ws.opentracing.tracer; type="ibm-spi"
                 
-features=com.ibm.websphere.appserver.jaxrs-2.0; ibm.tolerates:=2.1, \
          com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:=2.0
          
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.opentracing, \
         com.ibm.websphere.appserver.thirdparty.opentracing; location:="dev/api/third-party/,lib/", \
         com.ibm.websphere.org.eclipse.microprofile.opentracing.1.0; location:="dev/api/stable/,lib/"
         
kind=ga
edition=core
