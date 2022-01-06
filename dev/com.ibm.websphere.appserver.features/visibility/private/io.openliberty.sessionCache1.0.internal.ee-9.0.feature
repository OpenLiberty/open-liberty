-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.sessionCache1.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0"

-bundles= com.ibm.ws.session.jakarta, \
  		  com.ibm.ws.session.cache.jakarta, \
  		  com.ibm.ws.session.store.jakarta, \
  		  com.ibm.websphere.javaee.jcache.1.1.jakarta; mavenCoordinates="javax.cache:cache-api:1.1.0"

kind=ga
edition=core
