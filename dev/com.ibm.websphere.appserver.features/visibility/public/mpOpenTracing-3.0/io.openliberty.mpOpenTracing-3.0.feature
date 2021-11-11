-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-3.0
Subsystem-Name: MicroProfile OpenTracing 3.0
IBM-API-Package: \
    org.eclipse.microprofile.opentracing; type="stable"
-features=io.openliberty.opentracing-3.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.org.eclipse.microprofile.opentracing-3.0, \
  io.openliberty.mpCompatible-5.0
-bundles=\
    io.openliberty.microprofile.opentracing.2.0.internal.jakarta; apiJar=false; location:="lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel
