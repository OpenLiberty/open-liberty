-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jCacheContainer-1.1
visibility=public
IBM-ShortName: jCacheContainer-1.1
Manifest-Version: 1.0
Subsystem-Name: JCache via Bells
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
 com.ibm.websphere.appserver.bells-1.0, \
 com.ibm.websphere.appserver.classloading-1.0
-bundles=\
 com.ibm.websphere.javaee.jcache.1.1
kind=noship
edition=full
