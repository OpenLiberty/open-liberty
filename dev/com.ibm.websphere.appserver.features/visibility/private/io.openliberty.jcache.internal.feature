-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcache.internal-1.1
visibility=private
singleton=true
IBM-ShortName: jcache-1.1
Subsystem-Version: 1.1
Subsystem-Name: Java Caching (JCache) 1.1

# TODO: Not sure we want to expose these.
IBM-API-Package: \
  javax.cache; type="spec", \
  javax.cache.annotation; type="spec", \
  javax.cache.configuration; type="spec", \
  javax.cache.event; type="spec", \
  javax.cache.expiry; type="spec", \
  javax.cache.integration; type="spec", \
  javax.cache.management; type="spec", \
  javax.cache.processor; type="spec", \
  javax.cache.spi; type="spec"
  
-features=\
  com.ibm.websphere.appserver.classloading-1.0
  
# TODO: com.ibm.ws.security.token only for deserialization
-bundles=\
  io.openliberty.jcache.internal, \
  com.ibm.ws.security.token, \
  com.ibm.websphere.javaee.jcache.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.cache:cache-api:1.1.0"
kind=noship
edition=full
