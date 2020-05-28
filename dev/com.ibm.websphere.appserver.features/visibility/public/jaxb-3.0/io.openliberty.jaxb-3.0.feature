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
  io.openliberty.jakarta.jaxb-3.0
-files=\
  bin/jaxb/xjc.bat, \
  bin/jaxb/tools/ws-schemagen.jakarta.jar, \
  bin/jaxb/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  bin/jaxb/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  bin/jaxb/tools/ws-xjc.jakarta.jar, \
  bin/jaxb/schemagen.bat
kind=noship
edition=full
