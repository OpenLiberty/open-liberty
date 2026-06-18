-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.dataContainer-1.1
visibility=public
singleton=true
IBM-ShortName: dataContainer-1.1
IBM-API-Package: \
  jakarta.data; type="spec",\
  jakarta.data.exceptions; type="spec",\
  jakarta.data.metamodel; type="spec",\
  jakarta.data.metamodel.impl; type="spec",\
  jakarta.data.page; type="spec",\
  jakarta.data.page.impl; type="spec",\
  jakarta.data.repository; type="spec",\
  jakarta.data.spi; type="spec"
Subsystem-Name: Jakarta Data 1.1 Container
-features=\
  com.ibm.websphere.appserver.eeCompatible-12.0,\
  io.openliberty.cdi-5.0,\
  io.openliberty.jakarta.data-1.1
-bundles=\
  io.openliberty.data.internal.beandef
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
