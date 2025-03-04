-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-8.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health.javax-4.0,\
 com.ibm.websphere.appserver.mpConfig-2.0,\
 com.ibm.websphere.appserver.jsonp-1.1,\
 com.ibm.websphere.appserver.cdi-2.0,\
 io.openliberty.mpCompatible-4.0,\
 com.ibm.websphere.appserver.eeCompatible-8.0
-bundles=\
  io.openliberty.microprofile.health.4.0.internal; apiJar=false; location:="lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel 
