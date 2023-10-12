-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jndi-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-SPI-Package: \
  com.ibm.wsspi.anno.info, \
  com.ibm.wsspi.anno.util, \
  com.ibm.wsspi.anno.targets, \
  com.ibm.wsspi.anno.classsource, \
  com.ibm.wsspi.anno.service, \
  com.ibm.ws.anno.classsource.specification, \
  com.ibm.wsspi.adaptable.module.adapters, \
  com.ibm.wsspi.adaptable.module, \
  com.ibm.wsspi.artifact, \
  com.ibm.wsspi.artifact.equinox.module, \
  com.ibm.wsspi.artifact.factory.contributor, \
  com.ibm.wsspi.artifact.overlay, \
  com.ibm.ws.adaptable.module.structure, \
  com.ibm.wsspi.artifact.factory
IBM-ShortName: jndi-1.0
IBM-Process-Types: server, \
 client
Subsystem-Name: Java Naming and Directory Interface 1.0
-features=com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  com.ibm.websphere.appserver.anno-1.0; ibm.tolerates:="2.0"
-bundles=com.ibm.ws.jndi.url.contexts, \
 com.ibm.ws.org.apache.aries.jndi.core, \
 com.ibm.ws.org.apache.aries.jndi.api, \
 com.ibm.ws.jndi
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
