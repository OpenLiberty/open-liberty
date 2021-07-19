-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.zosLocalAdapters-1.0
visibility=public
IBM-API-Package: com.ibm.websphere.ola;type="ibm-api", \
 com.ibm.ws390.ola.jca;type="internal", \
 com.ibm.ws390.ola;type="internal"
IBM-ShortName: zosLocalAdapters-1.0
Subsystem-Name: z/OS Optimized Local Adapters 1.0
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.channelfw-1.0, \
 com.ibm.websphere.appserver.zosSecurity-1.0, \
 com.ibm.websphere.appserver.ejbLite-3.1; ibm.tolerates:=3.2, \
 com.ibm.websphere.appserver.jca-1.6; ibm.tolerates:=1.7
-bundles=com.ibm.ws.zos.channel.local, \
 com.ibm.websphere.ola, \
 com.ibm.ws.zos.ola, \
 com.ibm.ws.zos.channel.wola
-jars=com.ibm.websphere.appserver.api.zosLocalAdapters; location:=dev/api/ibm/
-files=lib/ola.rar, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.zosLocalAdapters_1.0-javadoc.zip
kind=ga
edition=zos
