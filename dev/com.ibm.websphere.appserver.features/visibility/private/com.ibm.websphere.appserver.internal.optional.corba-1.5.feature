-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.optional.corba-1.5
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: OMG CORBA APIs and RMI-IIOP API
-bundles=\
  com.ibm.ws.org.apache.yoko.corba.spec.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.osgi.1.5; require-java:="9",\
  com.ibm.ws.org.apache.servicemix.bundles.bcel.5.2; require-java:="9",\
  com.ibm.ws.org.apache.yoko.rmi.impl.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.core.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.util.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.rmi.spec.1.5; require-java:="9"
kind=ga
edition=core
