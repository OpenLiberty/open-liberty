-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaspic-2.0
visibility=public
IBM-API-Package: \
  jakarta.security.auth.message; type="spec", \
  jakarta.security.auth.message.callback; type="spec", \
  jakarta.security.auth.message.config; type="spec", \
  jakarta.security.auth.message.module; type="spec"
IBM-ShortName: jaspic-2.0
IBM-SPI-Package: \
  com.ibm.wsspi.security.jaspi; type="ibm-spi"
Subsystem-Name: Jakarta Authentication SPI for Containers (JASPIC) 2.0
-features=\
  io.openliberty.appSecurity-4.0, \
  io.openliberty.jaxb-3.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.jakarta.jaspic.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.auth.message:jakarta.security.auth.message-api:2.0.0-RC1", \
  io.openliberty.security.jaspic.2.0.internal
kind=beta
edition=core
-jars=io.openliberty.jaspic.2.0.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.jaspic_1.1-javadoc.zip
