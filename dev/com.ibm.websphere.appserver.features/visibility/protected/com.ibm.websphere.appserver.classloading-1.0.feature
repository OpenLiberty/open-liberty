-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.classloading-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-API-Package: \
  javax.activity; type=spec; require-java:=9
IBM-SPI-Package: \
  com.ibm.wsspi.classloading, \
  com.ibm.wsspi.library
IBM-Process-Types: server, client
-features=com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.dynamicBundle-1.0, \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=\
  com.ibm.websphere.javaee.activity.1.0; require-java:=9; location:="dev/api/spec/,lib/",\
  com.ibm.ws.classloading
-jars=com.ibm.websphere.appserver.spi.classloading; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.classloading_1.4-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
