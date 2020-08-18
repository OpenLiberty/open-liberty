-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaMail-2.0
visibility=public
singleton=true
IBM-ShortName: javaMail-2.0
Subsystem-Version: 2.0
Subsystem-Name: javaMail 2.0

IBM-API-Package: \
 jakarta.mail; type="spec", \
 jakarta.mail.internet; type="spec", \
 jakarta.mail.util; type="spec", \
 jakarta.mail.search; type="spec", \
 jakarta.mail.event; type="spec", \
 com.sun.mail; type="third-party", \
 com.sun.mail.auth; type="third-party", \
 com.sun.mail.imap; type="third-party", \
 com.sun.mail.imap.protocol; type="third-party", \
 com.sun.mail.iap; type="third-party", \
 com.sun.mail.pop3; type="third-party", \
 com.sun.mail.smtp; type="third-party", \
 com.sun.mail.util; type="third-party", \
 com.sun.mail.util.logging; type="third-party", \
 com.sun.mail.handlers; type="third-party"
 -features=\
  com.ibm.websphere.appserver.classloading-1.0,\
  com.ibm.websphere.appserver.injection-1.0,\
  com.ibm.websphere.appserver.jakarta.mail-2.0,\
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.jakarta.activation.2.0; location:="dev/api/spec/,lib/";mavenCoordinates="jakarta.activation:jakarta.activation-api:2.0", \
  com.ibm.ws.jakarta.mail.2.0, \
  com.ibm.ws.javamail.config
-jars=com.ibm.websphere.jakartaee.mail.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.mail:jakarta.mail-api:2.0.0", \
 com.ibm.websphere.appserver.thirdparty.mail-2.0; location:=dev/api/third-party/; mavenCoordinates="com.sun.mail:jakarta.mail:2.0.0"
kind=noship
edition=core
