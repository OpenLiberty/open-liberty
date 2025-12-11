-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-9.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health-4.0,\
 io.openliberty.mpConfig-3.0,\
 io.openliberty.jsonp-2.0,\
 io.openliberty.cdi-3.0,\
 io.openliberty.mpCompatible-5.0,\
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.microprofile.health.4.0.internal.jakarta; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel 
