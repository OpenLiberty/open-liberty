-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.opentracingMock-0.30
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: opentracingMock-0.30
Subsystem-Name: Opentracing Mock Tracer 0.30.0
                 
-features=com.ibm.websphere.appserver.opentracing-1.0
          
-bundles=com.ibm.ws.require.java8, \
         com.ibm.ws.opentracing.mock, \
         com.ibm.ws.opentracing.cdi
         
kind=noship
edition=core
