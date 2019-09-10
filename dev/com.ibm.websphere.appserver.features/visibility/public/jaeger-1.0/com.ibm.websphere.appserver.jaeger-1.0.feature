-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaeger-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: jaeger-1.0
Subsystem-Name: Jaeger Tracing 1.0
-features=com.ibm.websphere.appserver.mpOpenTracing-1.3
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.microprofile.opentracing.jaeger, \
         com.ibm.ws.microprofile.opentracing.jaeger.adapter, \
         com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl
kind=beta
edition=core
