-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpOpenTracing-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpOpenTracing-2.0
Subsystem-Name: MicroProfile OpenTracing 2.0
IBM-API-Package: \
    org.eclipse.microprofile.opentracing; type="stable"
-features=\
    io.openliberty.opentracing-2.0, \
    io.openliberty.org.eclipse.microprofile.opentracing-2.0, \
    io.openliberty.mpConfig-2.0
-bundles=\
    io.openliberty.microprofile.opentracing.2.0.internal
kind=beta
edition=core
WLP-Activation-Type: parallel
