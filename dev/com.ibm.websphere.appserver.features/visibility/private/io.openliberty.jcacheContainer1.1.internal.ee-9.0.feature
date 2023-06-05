-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jcacheContainer1.1.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0"
-bundles=\
  com.ibm.websphere.javaee.jcache.1.1.core.jakarta; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.cache:cache-api:1.1.0"
kind=noship
edition=full
