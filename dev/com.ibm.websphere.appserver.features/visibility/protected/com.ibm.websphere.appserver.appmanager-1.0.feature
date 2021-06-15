-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appmanager-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-API-Package: \
 com.ibm.websphere.application; type="ibm-api", \
 com.ibm.websphere.filemonitor; type="ibm-api", \
 com.ibm.websphere.kernel.server; type="ibm-api", \
 com.ibm.websphere.runtime.update; type="ibm-api", \
 com.ibm.websphere.security; type="ibm-api", \
 com.ibm.websphere.security.auth; type="ibm-api", \
 com.ibm.websphere.security.cred; type="ibm-api", \
 com.ibm.wsspi.security.registry; type="ibm-api"
IBM-SPI-Package: \
 com.ibm.wsspi.application.handler
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.app.manager, \
 com.ibm.ws.app.manager.ready; start-phase:=APPLICATION
-jars=com.ibm.websphere.appserver.api.basics; location:="dev/api/ibm/,lib/",\
 com.ibm.websphere.appserver.spi.application; location:=dev/spi/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.basics_1.4-javadoc.zip, \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.application_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
