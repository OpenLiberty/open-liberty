-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.optional.jaxb-2.2
visibility=private
IBM-App-ForceRestart: uninstall, install
IBM-Process-Types: client, server
Subsystem-Name: Java XML Bindings 2.2 for Java 9 and above
IBM-API-Package: \
  javax.activation; type="spec"; require-java:="9", \
  javax.xml.bind; type="spec"; require-java:="9", \
  javax.xml.bind.annotation; type="spec"; require-java:="9", \
  javax.xml.bind.annotation.adapters; type="spec"; require-java:="9", \
  javax.xml.bind.attachment; type="spec"; require-java:="9", \
  javax.xml.bind.helpers; type="spec"; require-java:="9", \
  javax.xml.bind.util; type="spec"; require-java:="9"
-features=\
  com.ibm.websphere.appserver.internal.optional.jaxb-2.2
kind=ga
edition=core
