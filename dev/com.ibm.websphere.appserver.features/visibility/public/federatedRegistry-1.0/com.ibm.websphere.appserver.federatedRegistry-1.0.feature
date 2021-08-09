-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.federatedRegistry-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: federatedRegistry-1.0
Subsystem-Name: Federated User Registry 1.0
IBM-SPI-Package: \
  com.ibm.wsspi.security.wim; type="ibm-spi", \
  com.ibm.wsspi.security.wim.exception; type="ibm-spi", \
  com.ibm.wsspi.security.wim.model; type="ibm-spi"
-features=com.ibm.websphere.appserver.wimcore-1.0, \
  com.ibm.websphere.appserver.securityInfrastructure-1.0
-bundles=\
  com.ibm.websphere.security, \
  com.ibm.ws.security.registry, \
  com.ibm.ws.security.wim.registry

-jars=com.ibm.websphere.appserver.spi.federatedRepository; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.federatedRepository_1.2-javadoc.zip
 
kind=ga
edition=core
