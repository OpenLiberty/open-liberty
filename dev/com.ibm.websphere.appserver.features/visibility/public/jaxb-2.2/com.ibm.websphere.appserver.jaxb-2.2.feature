-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxb-2.2
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: javax.xml.bind;  type="spec", \
 javax.xml.bind.annotation;  type="spec", \
 javax.xml.bind.annotation.adapters;  type="spec", \
 javax.xml.bind.attachment;  type="spec", \
 javax.xml.bind.helpers;  type="spec", \
 javax.xml.bind.util;  type="spec"
IBM-ShortName: jaxb-2.2
IBM-Process-Types: client, \
 server
Subsystem-Name: Java XML Bindings 2.2
-features=com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.websphere.javaee.jaxb.2.2; location:="dev/api/spec/,lib/", \
 com.ibm.ws.org.apache.geronimo.osgi.registry.1.1, \
 com.ibm.ws.jaxb.tools.2.2.10
-jars=com.ibm.ws.jaxb.tools.2.2.10; location:=lib/
-files=bin/jaxb/xjc.bat, \
 bin/jaxb/tools/ws-schemagen.jar, \
 bin/jaxb/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxb/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxb/tools/ws-xjc.jar, \
 bin/jaxb/schemagen.bat
kind=ga
edition=base
