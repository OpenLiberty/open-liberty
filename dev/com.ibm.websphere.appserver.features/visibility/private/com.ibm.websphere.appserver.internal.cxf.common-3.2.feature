-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.cxf.common-3.2
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Apache CXF 3.2 Common Feature for JAX-RS and JAX-WS
-bundles=\
 com.ibm.websphere.org.osgi.service.http; location:="dev/api/spec/,lib/", \
 com.ibm.ws.cxf.client, \
 com.ibm.ws.org.apache.cxf.cxf.core.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.hc.3.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.org.apache.xml.resolver.1.2
kind=ga
edition=core
WLP-Activation-Type: parallel
