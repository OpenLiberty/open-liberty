-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-11.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health-4.0,\
 io.openliberty.mpConfig-3.1,\
 io.openliberty.jsonp-2.1,\
 io.openliberty.cdi-4.1,\
 io.openliberty.mpCompatible-7.0,\
 com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
  io.openliberty.microprofile.health.4.0.internal.jakarta; apiJar=false; location:="lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel 
