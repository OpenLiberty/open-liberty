-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcache.internal-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
Subsystem-Version: 1.1
Subsystem-Name: Java Caching (JCache) 1.1
IBM-SPI-Package: \
  javax.cache, \
  javax.cache.annotation, \
  javax.cache.configuration, \
  javax.cache.event, \
  javax.cache.expiry, \
  javax.cache.integration, \
  javax.cache.management, \
  javax.cache.processor, \
  javax.cache.spi, \
  io.openliberty.jcache
-features=\
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  io.openliberty.jcache.internal, \
  com.ibm.ws.serialization
kind=ga
edition=core
