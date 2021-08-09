-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.sessionCache1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private

-features=\
  io.openliberty.servlet.api-4.0; ibm.tolerates:="3.1,3.0"; apiJar=false

-bundles= com.ibm.ws.session, \
  		  com.ibm.ws.session.cache, \
  		  com.ibm.ws.session.store, \
  		  com.ibm.websphere.javaee.jcache.1.1; mavenCoordinates="javax.cache:cache-api:1.1.0"

kind=ga
edition=core
