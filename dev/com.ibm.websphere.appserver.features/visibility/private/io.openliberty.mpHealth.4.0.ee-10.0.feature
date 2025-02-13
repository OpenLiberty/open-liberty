-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-10.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health-4.0,\
 io.openliberty.mpConfig-3.1,\
 io.openliberty.jsonp-2.1,\
 io.openliberty.cdi-4.0,\
 io.openliberty.mpCompatible-6.1,\
 com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  io.openliberty.microprofile.health.4.0.internal.jakarta; apiJar=false; location:="lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel 
