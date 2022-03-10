-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.distributedSecurityCache-1.0
visibility=public
singleton=true
IBM-ShortName: distributedSecurityCache-1.0
Subsystem-Version: 1.0
Subsystem-Name: Distributed Security Cache Support 1.0

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

kind=beta
edition=core
