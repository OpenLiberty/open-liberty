-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appSecurity-4.0
visibility=public
IBM-API-Package: jakarta.security.enterprise; type="spec", \
 jakarta.security.enterprise.authentication.mechanism.http; type="spec", \
 jakarta.security.enterprise.credential; type="spec", \
 jakarta.security.enterprise.identitystore; type="spec", \
 jakarta.security.auth.message; type="spec", \
 jakarta.security.auth.message.callback; type="spec", \
 jakarta.security.auth.message.config; type="spec", \
 jakarta.security.auth.message.module; type="spec"

IBM-ShortName: appSecurity-4.0
Subsystem-Name: Application Security 4.0
-features=io.openliberty.cdi-3.0, \
 com.ibm.websphere.appserver.el-4.0, \
 com.ibm.websphere.appserver.security-1.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.jakarta.security.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.enterprise:jakarta.security.enterprise-api:2.0.0-RC2", \
 io.openliberty.security.jakartasec.2.0.internal, \
 io.openliberty.security.jakartasec.2.0.internal.cdi, \
 io.openliberty.security.authentication.internal.tai, \
 io.openliberty.jakarta.jaspic.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.auth.message:jakarta.security.auth.message-api:2.0.0-RC1", \
 io.openliberty.security.jaspic.2.0.internal
kind=noship
edition=full
-jars=io.openliberty.jaspic.2.0.spi; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.jaspic_1.1-javadoc.zip
