-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxb-2.3
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  javax.activation; type="spec"; require-java:="9", \
  javax.xml.bind;  type="spec", \
  javax.xml.bind.annotation;  type="spec", \
  javax.xml.bind.annotation.adapters;  type="spec", \
  javax.xml.bind.attachment;  type="spec", \
  javax.xml.bind.helpers;  type="spec", \
  javax.xml.bind.util;  type="spec"
IBM-ShortName: jaxb-2.3
IBM-Process-Types: client, \
 server
Subsystem-Name: Java XML Bindings 2.3
-features=com.ibm.websphere.appserver.optional.jaxb-2.3, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  com.ibm.websphere.javaee.activation.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.activation:activation:1.1", \
  com.ibm.websphere.javaee.jaxb.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.bind:jaxb-api:2.3.0"
-files=\
  bin/jaxb/xjc.bat, \
  bin/jaxb/tools/ws-schemagen.jar, \
  bin/jaxb/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  bin/jaxb/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  bin/jaxb/tools/ws-xjc.jar, \
  bin/jaxb/schemagen.bat
kind=noship
edition=full
WLP-Activation-Type: parallel
