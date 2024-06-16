-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jcacheContainer1.1.internal.ee-6.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0,7.0"
-bundles=\
  com.ibm.websphere.javaee.jcache.1.1.core; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.cache:cache-api:1.1.0"
kind=ga
edition=full
