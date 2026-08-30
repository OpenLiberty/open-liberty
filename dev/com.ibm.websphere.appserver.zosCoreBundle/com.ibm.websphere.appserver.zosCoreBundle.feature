-include= ~../ant_build/resources/bnd/feature.props, ~build/gen.features

symbolicName = com.ibm.websphere.appserver.zosCoreBundle
visibility = install
singleton = true

IBM-ShortName: zosCoreBundle

zos.features: com.ibm.websphere.appserver.zosSecurity-1.0, \
 com.ibm.websphere.appserver.zosTransaction-1.0, \
 com.ibm.websphere.appserver.zosWlm-1.0, \
 com.ibm.websphere.appserver.zosLocalAdapters-1.0, \
 com.ibm.websphere.appserver.zosRequestLogging-1.0
-features: ${core.features}, \
 ${zos.features}

Subsystem-Name: z/OS Core Bundle
edition=full
kind=noship
