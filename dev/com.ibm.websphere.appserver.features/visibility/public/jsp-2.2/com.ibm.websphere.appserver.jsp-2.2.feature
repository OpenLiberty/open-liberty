-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsp-2.2
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.servlet.jsp;  type="spec", \
 javax.servlet.jsp.el;  type="spec", \
 javax.servlet.jsp.resources;  type="spec", \
 javax.servlet.jsp.tagext;  type="spec", \
 javax.servlet.jsp.jstl.sql;  type="spec", \
 javax.servlet.jsp.jstl.tlv;  type="spec", \
 javax.servlet.jsp.jstl.core;  type="spec", \
 javax.servlet.jsp.jstl.fmt;  type="spec", \
 javax.el;  type="spec", \
 org.apache.taglibs.standard;  type="internal", \
 org.apache.taglibs.standard.tag.common.core;  type="internal", \
 org.apache.taglibs.standard.tag.common.fmt;  type="internal", \
 org.apache.taglibs.standard.tag.common.sql;  type="internal", \
 org.apache.taglibs.standard.tag.common.xml;  type="internal", \
 org.apache.taglibs.standard.tag.el.core;  type="internal", \
 org.apache.taglibs.standard.tag.el.fmt;  type="internal", \
 org.apache.taglibs.standard.tag.el.sql;  type="internal", \
 org.apache.taglibs.standard.tag.el.xml;  type="internal", \
 org.apache.taglibs.standard.functions;  type="internal", \
 org.apache.taglibs.standard.tag.rt.core;  type="internal", \
 org.apache.taglibs.standard.tag.rt.fmt;  type="internal", \
 org.apache.taglibs.standard.tag.rt.sql;  type="internal", \
 org.apache.taglibs.standard.tag.rt.xml;  type="internal", \
 org.apache.el;  type="internal", \
 org.apache.el.lang;  type="internal", \
 org.apache.jasper.el;  type="internal"
IBM-ShortName: jsp-2.2
IBM-SPI-Package: com.ibm.wsspi.jsp.taglib.config
Subsystem-Name: JavaServer Pages 2.2
-features=com.ibm.websphere.appserver.javax.jsp-2.2, \
 com.ibm.websphere.appserver.javax.el-2.2, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:=3.1, \
 com.ibm.websphere.appserver.javaeeCompatible-6.0
-bundles=com.ibm.ws.org.eclipse.jdt.core.3.10.2.v20160712-0000, \
 com.ibm.ws.jsp.factories, \
 com.ibm.websphere.javaee.jstl.1.2; location:="dev/api/spec/,lib/", \
 com.ibm.ws.jsp.jasper, \
 com.ibm.ws.jsp, \
 com.ibm.ws.jsp.jstl.facade, \
 com.ibm.ws.org.apache.jasper.el.2.2
-jars=com.ibm.websphere.appserver.spi.jsp; location:=dev/spi/ibm/, \
 com.ibm.websphere.javaee.jsp.tld.2.2; location:=dev/api/spec/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.jsp_1.0-javadoc.zip
kind=ga
edition=core
