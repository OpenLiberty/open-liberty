-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: data-1.0
IBM-API-Package: \
  jakarta.data; type="spec",\
  jakarta.data.exceptions; type="spec",\
  jakarta.data.metamodel; type="spec",\
  jakarta.data.metamodel.impl; type="spec",\
  jakarta.data.page; type="spec",\
  jakarta.data.page.impl; type="spec",\
  jakarta.data.repository; type="spec",\
  jakarta.data.spi; type="spec"
Subsystem-Name: Jakarta Data 1.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0,\
  io.openliberty.cdi-4.1,\
  io.openliberty.jakarta.data-1.0
-bundles=\
  io.openliberty.data.internal,\
  io.openliberty.data.internal.beandef,\
  io.openliberty.data.1.0.internal
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-11.0
