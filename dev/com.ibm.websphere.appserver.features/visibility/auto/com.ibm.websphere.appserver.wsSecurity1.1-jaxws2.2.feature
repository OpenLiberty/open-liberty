-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wsSecurity1.1-jaxws2.2
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.wsSecurity-1.1))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.internal.slf4j-1.7.7,\
  com.ibm.websphere.appserver.wss4j-1.0
-bundles=com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.ws.org.opensaml.opensaml.2.6.1, \
  com.ibm.ws.prereq.wsdl4j.1.6.2, \
  com.ibm.ws.net.sf.ehcache.core.2.5.2, \
  com.ibm.ws.org.apache.cxf.ws.mex.2.6.2, \
  com.ibm.ws.wssecurity, \
  com.ibm.ws.org.apache.cxf.ws.security.2.6.2, \
  com.ibm.ws.org.opensaml.openws.1.5.6, \
  io.openliberty.org.apache.commons.logging
kind=ga
edition=base
