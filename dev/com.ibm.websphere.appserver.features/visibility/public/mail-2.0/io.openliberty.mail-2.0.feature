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
  com.sun.mail.handlers; type="third-party", \
  jakarta.activation; type="spec", \
  jakarta.annotation; type="spec", \
  jakarta.annotation.security; type="spec", \
  jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
  com.ibm.wsspi.adaptable.module, \
  com.ibm.ws.adaptable.module.structure, \
  com.ibm.wsspi.adaptable.module.adapters, \
  com.ibm.wsspi.artifact, \
  com.ibm.wsspi.artifact.factory, \
  com.ibm.wsspi.artifact.factory.contributor, \
  com.ibm.wsspi.artifact.overlay, \
  com.ibm.wsspi.artifact.equinox.module, \
  com.ibm.wsspi.anno.classsource, \
  com.ibm.wsspi.anno.info, \
  com.ibm.wsspi.anno.service, \
  com.ibm.wsspi.anno.targets, \
  com.ibm.wsspi.anno.util, \
  com.ibm.ws.anno.classsource.specification
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.mail-2.0, \
  com.ibm.websphere.appserver.injection-2.0
-bundles=\
  io.openliberty.mail.2.0.internal, \
  com.ibm.ws.javamail.config
-jars=io.openliberty.mail.2.0.thirdparty; location:=dev/api/third-party/; mavenCoordinates="com.sun.mail:jakarta.mail:2.0.0"
kind=ga
edition=core
WLP-InstantOn-Enabled: true
