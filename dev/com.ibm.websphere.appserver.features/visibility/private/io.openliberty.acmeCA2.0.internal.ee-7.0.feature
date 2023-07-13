-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.acmeCA2.0.internal.ee-7.0
WLP-DisableAllFeatures-OnConflict: false
Subsystem-Version: 7.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="3.1"
-bundles=\
  com.ibm.ws.security.acme, \
  io.openliberty.org.bouncycastle.bcpkix-jdk18on, \
  io.openliberty.org.bouncycastle.bcprov-jdk18on, \
  io.openliberty.org.bouncycastle.bcutil-jdk18on
kind=ga
edition=base
