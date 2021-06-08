-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jcache.autoapi-1.1
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jcache.internal-1.1))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.noShip-1.0))"
IBM-Install-Policy: when-satisfied

#
# These packages need to be exposed so that the JCache provider library
# classloader can have access to them.
#
# Infinispan needs access to the JCacheObject in io.openliberty.jcache; otherwise,
# we get ClassNotFoundExceptions.
#
# TODO Talk with Dave Z about the possibility of replacing these before we GA.
#
IBM-API-Package: \
  javax.cache; type="internal", \
  javax.cache.annotation; type="internal", \
  javax.cache.configuration; type="internal", \
  javax.cache.event; type="internal", \
  javax.cache.expiry; type="internal", \
  javax.cache.integration; type="internal", \
  javax.cache.management; type="internal", \
  javax.cache.processor; type="internal", \
  javax.cache.spi; type="internal", \
  io.openliberty.jcache; type="internal"

#
# TODO Before GA, this feature should be removed and the exposure of the javax.cache 
#      APIs should be moved to the io.openliberty.jcache.autoapi-1.1 feature. This
#      feature is just here to stage for noship/beta without exposing these APIs before
#      GA.
#
kind=noship
edition=full
