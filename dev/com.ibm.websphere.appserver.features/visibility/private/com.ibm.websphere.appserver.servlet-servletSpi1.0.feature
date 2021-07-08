-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.servlet-servletSpi1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-jars=com.ibm.websphere.appserver.spi.servlet; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.servlet_2.10-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
