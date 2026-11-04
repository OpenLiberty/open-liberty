-include= ~../ant_build/resources/bnd/feature.props, ~build/gen.features

symbolicName = com.ibm.websphere.appserver.zosBundle
visibility = install
singleton = true

IBM-ShortName: zosBundle
-features: ${core.features}, \
 ${base.features}, \
 ${nd.member.features}, \
 ${nd.controller.features}, \
 ${zos.features}

Subsystem-Name: z/OS Bundle
edition=zos
kind=ga
