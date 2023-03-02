-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mail-2.1
visibility=public
singleton=true
IBM-ShortName: mail-2.1
Subsystem-Version: 2.1
Subsystem-Name: Jakarta Mail 2.1
WLP-AlsoKnownAs: javaMail-2.1
WLP-Activation-Type: parallel
IBM-API-Package: \
  jakarta.mail; type="spec", \
  jakarta.mail.internet; type="spec", \
  jakarta.mail.util; type="spec", \
  jakarta.mail.search; type="spec", \
  jakarta.mail.event; type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.mail-2.1, \
  io.openliberty.activation.internal-2.1, \
  com.ibm.websphere.appserver.injection-2.0
-bundles=\
  io.openliberty.mail.2.1.internal, \
  io.openliberty.org.eclipse.angus.mail, \
  com.ibm.ws.javamail.config
kind=beta
edition=core
