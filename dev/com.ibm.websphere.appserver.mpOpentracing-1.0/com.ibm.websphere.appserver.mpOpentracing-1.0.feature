-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpentracing-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpentracing-1.0
Subsystem-Name: Microprofile Opentracing 1.0

IBM-API-Package: org.eclipse.microprofile.opentracing; type="stable"
                 
-features=com.ibm.websphere.appserver.opentracing-1.0, \
          com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-1.0
          
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.opentracing.cdi
         
kind=ga
edition=core

