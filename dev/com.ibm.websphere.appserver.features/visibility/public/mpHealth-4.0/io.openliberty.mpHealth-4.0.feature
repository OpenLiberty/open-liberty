-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.health;  type="stable", \
  org.eclipse.microprofile.health.spi;  type="stable"
IBM-ShortName: mpHealth-4.0
Subsystem-Name: MicroProfile Health 4.0
# io.openliberty.mpCompatible-x.x comes from io.openliberty.mpConfig features
-features=io.openliberty.org.eclipse.microprofile.health-4.0,\
 io.openliberty.cdi-3.0; ibm.tolerates:="4.0,4.1",\
 com.ibm.websphere.appserver.jndi-1.0,\
 com.ibm.websphere.appserver.json-1.0,\
 io.openliberty.mpConfig-3.0; ibm.tolerates:="3.1",\
 io.openliberty.jsonp-2.0; ibm.tolerates:="2.1",\
 io.openliberty.webBundle.internal-1.0
-bundles=io.openliberty.microprofile.health.4.0.internal.jakarta; apiJar=false; location:="lib/", \
io.openliberty.microprofile.health.internal.common, \
 com.ibm.ws.org.joda.time.1.6.2
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-5.0,microProfile-6.0,microProfile-6.1,microProfile-7.0
