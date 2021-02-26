-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.artifact-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-SPI-Package: com.ibm.wsspi.adaptable.module, \
 com.ibm.ws.adaptable.module.structure, \
 com.ibm.wsspi.adaptable.module.adapters, \
 com.ibm.wsspi.artifact, \
 com.ibm.wsspi.artifact.factory, \
 com.ibm.wsspi.artifact.factory.contributor, \
 com.ibm.wsspi.artifact.overlay, \
 com.ibm.wsspi.artifact.equinox.module
IBM-Process-Types: server, \
 client
-bundles=com.ibm.ws.artifact; start-phase:=CONTAINER_EARLY, \
 com.ibm.ws.artifact.loose, \
 com.ibm.ws.adaptable.module, \
 com.ibm.ws.artifact.url; start-phase:=CONTAINER_EARLY, \
 com.ibm.ws.classloading.configuration, \
 com.ibm.ws.artifact.zip; start-phase:=CONTAINER_EARLY, \
 com.ibm.ws.artifact.overlay, \
 com.ibm.ws.artifact.bundle, \
 com.ibm.ws.artifact.equinox.module, \
 com.ibm.ws.artifact.file
-jars=com.ibm.websphere.appserver.spi.artifact; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.artifact_1.2-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
