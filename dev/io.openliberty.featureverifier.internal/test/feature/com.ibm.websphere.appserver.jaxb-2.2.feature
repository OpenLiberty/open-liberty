-include= ../../../../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxb-2.2
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: javax.xml.bind;  type="spec", \
 javax.xml.bind.annotation;  type="spec", \
 javax.xml.bind.annotation.adapters;  type="spec", \
 javax.xml.bind.attachment;  type="spec", \
 javax.xml.bind.helpers;  type="spec", \
 javax.xml.bind.util;  type="spec", \
 com.ibm.xml.xlxp2.jaxb.unmarshal.impl;type="internal", \
 com.ibm.xml.xlxp2.jaxb.marshal.impl;type="internal", \
 com.ibm.xml.xlxp2.jaxb.model;type="internal", \
 com.ibm.xml.xlxp2.datatype;type="internal", \
 com.ibm.xml.xlxp2.scan.util;type="internal"
IBM-ShortName: jaxb-2.2
IBM-Process-Types: client, \
 server
Subsystem-Name: Java XML Bindings 2.2
-features=com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.websphere.javaee.jaxb.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.bind:jaxb-api:2.2.12", \
 com.ibm.ws.org.apache.geronimo.osgi.registry.1.1, \
 com.ibm.ws.jaxb.tools.2.2.3, \
 com.ibm.ws.xlxp.1.5.3
-jars=com.ibm.ws.jaxb.tools.2.2.6; location:=lib/
-files=bin/jaxb/xjc.bat, \
 bin/jaxb/tools/ws-schemagen.jar, \
 bin/jaxb/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxb/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxb/tools/ws-xjc.jar, \
 bin/jaxb/schemagen.bat
kind=ga
edition=base
