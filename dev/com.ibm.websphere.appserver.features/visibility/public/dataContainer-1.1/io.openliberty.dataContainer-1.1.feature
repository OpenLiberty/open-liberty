-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.dataContainer-1.1
visibility=public
singleton=true
IBM-ShortName: dataContainer-1.1
IBM-API-Package: \
  jakarta.data; type="spec",\
  jakarta.data.constraint; type="spec",\
  jakarta.data.event; type="spec",\
  jakarta.data.exceptions; type="spec",\
  jakarta.data.expression; type="spec",\
  jakarta.data.metamodel; type="spec",\
  jakarta.data.metamodel.impl; type="spec",\
  jakarta.data.page; type="spec",\
  jakarta.data.page.impl; type="spec",\
  jakarta.data.repository; type="spec",\
  jakarta.data.repository.stateful; type="spec",\
  jakarta.data.restrict; type="spec",\
  jakarta.data.spi; type="spec",\
  jakarta.data.spi.expression.function; type="spec",\
  jakarta.data.spi.expression.literal; type="spec",\
  jakarta.data.spi.expression.path; type="spec"
Subsystem-Name: Jakarta Data 1.1 Container
-features=\
  com.ibm.websphere.appserver.eeCompatible-12.0,\
  io.openliberty.cdi-5.0,\
  io.openliberty.jakarta.data-1.1
-bundles=\
  io.openliberty.data.internal.beandef
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
