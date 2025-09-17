-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcache.internal1.1.ee-8.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
#
# CDI is optionally required by the JCache libraries.
#
-features=\
  com.ibm.websphere.appserver.eeCompatible-8.0
#  com.ibm.websphere.appserver.javax.cdi-2.0
-bundles=\
  com.ibm.websphere.javaee.jcache.1.1.core
kind=ga
edition=core
WLP-Activation-Type: parallel
