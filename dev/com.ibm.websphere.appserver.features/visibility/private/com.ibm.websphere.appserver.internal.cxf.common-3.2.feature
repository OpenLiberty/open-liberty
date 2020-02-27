-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.cxf.common-3.2
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Apache CXF 3.2 Common Feature for JAX-RS and JAX-WS
-bundles=com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.org.apache.cxf.cxf.core.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.3.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.websphere.org.osgi.service.http; location:="dev/api/spec/,lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
