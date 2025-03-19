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
-features=  \
 io.openliberty.mpHealth.4.0.ee-9.0; ibm.tolerates:= "10.0, 11.0, 8.0, 7.0",\
 com.ibm.websphere.appserver.jndi-1.0,\
 com.ibm.websphere.appserver.json-1.0,\
 io.openliberty.webBundle.internal-1.0
-bundles= \
 com.ibm.ws.org.joda.time.1.6.2,\
 io.openliberty.microprofile.health.internal.common
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-5.0,microProfile-6.0,microProfile-6.1,microProfile-7.0
