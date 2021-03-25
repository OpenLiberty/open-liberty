-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionCache-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: sessionCache-1.0
Manifest-Version: 1.0
Subsystem-Name: JCache Session Persistence 1.0
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
  com.ibm.websphere.appserver.sessionStore-1.0.0.JCache, \
  com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.servlet.api-4.0; ibm.tolerates:="3.1,3.0,5.0"; apiJar=false, \
  com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1,2.0", \
  io.openliberty.sessionCache1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  com.ibm.websphere.security
kind=ga
edition=core
