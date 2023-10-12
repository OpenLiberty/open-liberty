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
  jakarta.mail.event; type="spec", \
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
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.jakarta.mail-2.1, \
  io.openliberty.activation.internal-2.1, \
  com.ibm.websphere.appserver.injection-2.0
-bundles=\
  io.openliberty.mail.2.1.internal, \
  io.openliberty.org.eclipse.angus.mail, \
  com.ibm.ws.javamail.config
kind=ga
edition=core
WLP-InstantOn-Enabled: true
