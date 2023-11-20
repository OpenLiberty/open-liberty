-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthentication-2.0
visibility=public
singleton=true
IBM-API-Package: \
  jakarta.security.auth.message; type="spec", \
  jakarta.security.auth.message.callback; type="spec", \
  jakarta.security.auth.message.config; type="spec", \
  jakarta.security.auth.message.module; type="spec"
IBM-ShortName: appAuthentication-2.0
WLP-AlsoKnownAs: jaspic-2.0
IBM-SPI-Package: \
  com.ibm.wsspi.security.jaspi; type="ibm-spi"
Subsystem-Name: Jakarta Authentication 2.0
-features=io.openliberty.xmlBinding-3.0, \
  io.openliberty.appSecurity-4.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.servlet.internal-5.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.security.jaspic.2.0.internal
-jars=io.openliberty.jaspic.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/io.openliberty.jaspic.spi_1.1-javadoc.zip
kind=ga
edition=core
WLP-InstantOn-Enabled: true
