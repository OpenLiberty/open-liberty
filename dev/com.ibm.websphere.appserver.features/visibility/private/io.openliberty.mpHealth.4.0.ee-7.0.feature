-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth.4.0.ee-7.0
singleton=true
-features=\
 io.openliberty.org.eclipse.microprofile.health.javax-4.0,\
 com.ibm.websphere.appserver.mpConfig-1.3,\
 com.ibm.websphere.appserver.jsonp-1.0,\
 com.ibm.websphere.appserver.cdi-1.2,\
 io.openliberty.mpCompatible-0.0,\
 com.ibm.websphere.appserver.contextService-1.0,\
 com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=\
  io.openliberty.microprofile.health.4.0.internal; apiJar=false; location:="lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel 
