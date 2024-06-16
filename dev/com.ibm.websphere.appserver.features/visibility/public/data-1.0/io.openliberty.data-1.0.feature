-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data-1.0
visibility=public
singleton=true
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
#TODO io.openliberty.jakartaeePlatform-11.0 and stop tolerating EE 10
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.cdi-4.0; ibm.tolerates:="4.1",\
  io.openliberty.jakarta.data-1.0
-bundles=\
  io.openliberty.data.internal,\
  io.openliberty.data.internal.beandef,\
  io.openliberty.data.1.0.internal
 kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-11.0
