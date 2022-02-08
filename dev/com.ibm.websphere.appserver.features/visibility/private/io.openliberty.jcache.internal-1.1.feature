-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcache.internal-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-ShortName: jcache-1.1
Subsystem-Version: 1.1
Subsystem-Name: Java Caching (JCache) 1.1
-features=\
  com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0,7.0,9.0,10.0", \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jcache.internal1.1.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  io.openliberty.jcache.internal, \
  com.ibm.ws.serialization
#
# TODO Before OLGH 9005 feature GAs, the io.openliberty.jcache.autoapi-1.1 feature should be removed
#      and the exposure of the javax.cache APIs should be done in this feature.
#
kind=ga
edition=core
