-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.distributedMap-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.cache; type="ibm-api", \
 com.ibm.websphere.cache.exception; type="ibm-api", \
 com.ibm.websphere.exception; type="ibm-api", \
 com.ibm.ws.cache; type="internal", \
 com.ibm.ws.cache.eca; type="internal", \
 com.ibm.ws.cache.intf; type="internal", \
 com.ibm.ws.cache.config; type="internal", \
 com.ibm.ws.cache.spi; type="ibm-api", \
 com.ibm.wsspi.cache; type="ibm-api"
IBM-ShortName: distributedMap-1.0
Subsystem-Name: Distributed Map interface for Dynamic Caching 1.0
-features=\
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0,5.0", \
  io.openliberty.distributedMapInternal-1.0; ibm.tolerates:=2.0
-jars=com.ibm.websphere.appserver.api.distributedMap; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.distributedMap_2.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
