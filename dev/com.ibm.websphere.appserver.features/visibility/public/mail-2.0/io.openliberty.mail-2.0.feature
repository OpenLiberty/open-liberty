-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mail-2.0
visibility=public
singleton=true
IBM-ShortName: mail-2.0
Subsystem-Version: 2.0
Subsystem-Name: Jakarta Mail 2.0
WLP-AlsoKnownAs: javaMail-2.0
WLP-Activation-Type: parallel
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
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.mail-2.0, \
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.jakarta.activation-2.0
-bundles=\
  io.openliberty.mail.2.0.internal, \
  com.ibm.ws.javamail.config
-jars=io.openliberty.mail.2.0.thirdparty; location:=dev/api/third-party/; mavenCoordinates="com.sun.mail:jakarta.mail:2.0.0"
kind=beta
edition=core
