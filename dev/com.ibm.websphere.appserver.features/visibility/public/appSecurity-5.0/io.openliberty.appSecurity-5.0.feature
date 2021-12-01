-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appSecurity-5.0
visibility=public
singleton=true
IBM-API-Package: jakarta.security.enterprise; type="spec", \
 jakarta.security.enterprise.authentication.mechanism.http; type="spec", \
 jakarta.security.enterprise.credential; type="spec", \
 jakarta.security.enterprise.identitystore; type="spec", \
 jakarta.security.auth.message; type="spec", \
 jakarta.security.auth.message.callback; type="spec", \
 jakarta.security.auth.message.config; type="spec", \
 jakarta.security.auth.message.module; type="spec"

IBM-ShortName: appSecurity-5.0
Subsystem-Name: Application Security 5.0 (Jakarta Security 3.0)
-features=io.openliberty.cdi-4.0, \
  io.openliberty.jakarta.authentication-3.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.security-1.0, \
  io.openliberty.securityAPI.jakarta-1.0, \
  io.openliberty.jakarta.security.enterprise-3.0, \
  io.openliberty.expressionLanguage-5.0
-bundles=\
  io.openliberty.security.jakartasec.2.0.internal, \
  io.openliberty.security.jakartasec.2.0.internal.cdi, \
  io.openliberty.security.authentication.internal.filter, \
  io.openliberty.security.authentication.internal.tai, \
  io.openliberty.security.sso.internal, \
  io.openliberty.security.jaspic.2.0.internal
kind=noship
edition=full
-jars=io.openliberty.jaspic.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/io.openliberty.jaspic.spi_1.1-javadoc.zip
