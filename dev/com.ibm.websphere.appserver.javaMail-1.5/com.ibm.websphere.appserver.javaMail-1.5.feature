-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaMail-1.5
visibility=public
singleton=true
IBM-API-Package: javax.mail;  type="spec", \
 javax.mail.internet;  type="spec", \
 javax.mail.util;  type="spec", \
 javax.mail.search;  type="spec", \
 javax.mail.event;  type="spec", \
 com.sun.mail;  type="third-party", \
 com.sun.mail.auth;  type="third-party", \
 com.sun.mail.imap;  type="third-party", \
 com.sun.mail.imap.protocol;  type="third-party", \
 com.sun.mail.iap;  type="third-party", \
 com.sun.mail.pop3;  type="third-party", \
 com.sun.mail.smtp;  type="third-party", \
 com.sun.mail.util;  type="third-party", \
 com.sun.mail.util.logging;  type="third-party", \
 com.sun.mail.handlers;  type="third-party"
IBM-ShortName: javaMail-1.5
Subsystem-Version: 1.5
Subsystem-Name: JavaMail 1.5
-features=\
  com.ibm.websphere.appserver.classloading-1.0,\
  com.ibm.websphere.appserver.injection-1.0,\
  com.ibm.websphere.appserver.javax.mail-1.5
-bundles=\
  com.ibm.ws.javamail,\
  com.ibm.ws.javamail.config
-jars=com.ibm.websphere.javaee.mail.1.5; location:=dev/api/spec/, \
 com.ibm.websphere.appserver.thirdparty.mail; location:=dev/api/third-party/
kind=ga
edition=core
IBM-Maven-Dependency: com.sun.mail:javax.mail:1.6.0
