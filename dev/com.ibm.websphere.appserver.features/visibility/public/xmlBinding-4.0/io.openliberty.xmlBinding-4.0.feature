-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlBinding-4.0
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
IBM-ShortName: xmlBinding-4.0
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta XML Binding 4.0
-features=io.openliberty.xmlBinding.internal-4.0, \
  io.openliberty.activation.internal-2.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0
# These jars are used for the xmlBinding scripts below.
-jars=\
 io.openliberty.xmlBinding.3.0.internal.tools, \
 io.openliberty.jakarta.xmlBinding.3.0; location:="dev/api/spec/", \
 io.openliberty.jakarta.activation.2.0; location:="dev/api/spec/"
-files=\
 bin/xmlBinding/xjc.bat, \
 bin/xmlBinding/tools/ws-schemagen.jar, \
 bin/xmlBinding/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlBinding/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlBinding/tools/ws-xjc.jar, \
 bin/xmlBinding/schemagen.bat
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-AlsoKnownAs: jaxb-4.0
