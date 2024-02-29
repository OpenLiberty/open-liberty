-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcacheContainer-1.1
visibility=public
IBM-ShortName: jcacheContainer-1.1
Manifest-Version: 1.0
Subsystem-Name: JCache spec API and container integration
IBM-API-Package: \
 javax.cache; type="spec", \
 javax.cache.annotation; type="spec", \
 javax.cache.configuration; type="spec", \
 javax.cache.event; type="spec", \
 javax.cache.expiry; type="spec", \
 javax.cache.integration; type="spec", \
 javax.cache.management; type="spec", \
 javax.cache.processor; type="spec", \
 javax.cache.spi; type="spec"
-features=\
 io.openliberty.jcacheContainer1.1.internal.ee-6.0; ibm.tolerates:="9.0",\
 com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0,7.0,9.0,10.0,11.0",\
 com.ibm.websphere.appserver.classloading-1.0
kind=noship
edition=full
WLP-InstantOn-Enabled: true; type:=beta