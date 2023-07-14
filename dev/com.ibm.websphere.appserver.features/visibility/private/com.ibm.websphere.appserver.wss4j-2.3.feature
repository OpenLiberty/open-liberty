-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wss4j-2.3
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=com.ibm.ws.org.apache.santuario.xmlsec.2.2.0, \
 com.ibm.ws.com.google.guava, \
 com.ibm.ws.org.apache.wss4j.bindings, \
 com.ibm.ws.org.apache.wss4j.policy, \
 com.ibm.ws.org.apache.wss4j.ws.security.common, \
 com.ibm.ws.org.apache.wss4j.ws.security.dom, \
 com.ibm.ws.org.apache.wss4j.ws.security.policy.stax, \
 com.ibm.ws.org.apache.wss4j.ws.security.stax, \
 com.ibm.ws.org.apache.wss4j.ws.security.web, \
 com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
 com.ibm.ws.org.ehcache.ehcache.107.3.8.1, \
 com.ibm.ws.org.jasypt.jasypt.1.9.3, \
 com.ibm.ws.org.apache.neethi.3.1.1, \
 com.ibm.ws.org.joda.time.2.9.9, \
 com.ibm.ws.org.opensaml.opensaml.core.3.4.5, \
 com.ibm.ws.net.shibboleth.utilities.java.support.7.5.1, \
 io.openliberty.org.bouncycastle.bcpkix-jdk18on, \
 io.openliberty.org.bouncycastle.bcprov-jdk18on, \
 io.openliberty.org.bouncycastle.bcutil-jdk18on
kind=ga
edition=base
