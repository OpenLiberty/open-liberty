-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.optional.jaxb-2.3
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, install
IBM-Process-Types: client, server
Subsystem-Name: Java XML Bindings 2.3
IBM-API-Package: \
  javax.activation; type="spec", \
  javax.xml.bind; type="spec", \
  javax.xml.bind.annotation; type="spec", \
  javax.xml.bind.annotation.adapters; type="spec", \
  javax.xml.bind.attachment; type="spec", \
  javax.xml.bind.helpers; type="spec", \
  javax.xml.bind.util; type="spec"
-features=\
  com.ibm.websphere.appserver.internal.optional.jaxb-2.3
kind=noship
edition=full
