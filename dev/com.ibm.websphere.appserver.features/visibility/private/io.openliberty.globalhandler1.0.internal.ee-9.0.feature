-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.api-5.0; apiJar=false
-bundles=\
  io.openliberty.webservices.handler
-jars=\
  io.openliberty.globalhandler.spi; location:=dev/spi/ibm/
-files=\
  dev/spi/ibm/javadoc/io.openliberty.globalhandler.spi_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel