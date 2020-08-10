-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxb-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  jakarta.activation; type="spec", \
  jakarta.xml.bind;  type="spec", \
  jakarta.xml.bind.annotation;  type="spec", \
  jakarta.xml.bind.annotation.adapters;  type="spec", \
  jakarta.xml.bind.attachment;  type="spec", \
  jakarta.xml.bind.helpers;  type="spec", \
  jakarta.xml.bind.util;  type="spec"
IBM-ShortName: jaxb-3.0
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta XML Bindings 3.0
-features=\
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.jaxb-3.0
-bundles=\
  io.openliberty.jaxb.3.0.internal.tools
kind=beta
edition=core
