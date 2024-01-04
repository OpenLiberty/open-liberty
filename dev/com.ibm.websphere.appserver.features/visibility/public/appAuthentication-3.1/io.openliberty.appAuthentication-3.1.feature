-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthentication-3.1
visibility=public
singleton=true
IBM-API-Package: \
  jakarta.security.auth.message; type="spec", \
  jakarta.security.auth.message.callback; type="spec", \
  jakarta.security.auth.message.config; type="spec", \
  jakarta.security.auth.message.module; type="spec"
IBM-ShortName: appAuthentication-3.1
WLP-AlsoKnownAs: jaspic-3.1
IBM-SPI-Package: \
  com.ibm.wsspi.security.jaspi; type="ibm-spi"
Subsystem-Name: Jakarta Authentication 3.1
-features=io.openliberty.xmlBinding.internal-4.0, \
  io.openliberty.appSecurity-6.0, \
  com.ibm.websphere.appserver.servlet-6.1, \
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
  io.openliberty.security.jaspic.2.0.internal
-jars=io.openliberty.jaspic.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/io.openliberty.jaspic.spi_1.1-javadoc.zip
kind=noship
edition=full
WLP-InstantOn-Enabled: true
