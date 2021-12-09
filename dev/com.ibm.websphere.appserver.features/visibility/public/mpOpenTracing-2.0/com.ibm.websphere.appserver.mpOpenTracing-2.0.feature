-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpOpenTracing-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-2.0
Subsystem-Name: MicroProfile OpenTracing 2.0
IBM-API-Package: \
    org.eclipse.microprofile.opentracing; type="stable"
-features=com.ibm.websphere.appserver.opentracing-2.0, \
  com.ibm.websphere.appserver.mpConfig-2.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-2.0, \
  io.openliberty.mpCompatible-4.0
-bundles=\
    io.openliberty.microprofile.opentracing.2.0.internal
kind=ga
edition=core
WLP-Activation-Type: parallel
