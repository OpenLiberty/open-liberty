-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.dataContainer-1.0
visibility=public
singleton=true
IBM-ShortName: dataContainer-1.0
IBM-API-Package: \
  jakarta.data; type="spec",\
  jakarta.data.exceptions; type="spec",\
  jakarta.data.metamodel; type="spec",\
  jakarta.data.metamodel.impl; type="spec",\
  jakarta.data.page; type="spec",\
  jakarta.data.page.impl; type="spec",\
  jakarta.data.repository; type="spec",\
  jakarta.data.spi; type="spec"
Subsystem-Name: Jakarta Data 1.0 Container
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.cdi-4.0; ibm.tolerates:="4.1",\
  io.openliberty.jakarta.data-1.0
-bundles=\
  io.openliberty.data.internal.beandef
kind=ga
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-11.0
