-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data-1.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: data-1.1
# This feature temporarily serves as a place to keep around some function that
# did not make it into the Jakarta Data 1.0 specification, but could be added in
# the next version.
IBM-API-Package: \
  io.openliberty.data.repository; type="ibm-api",\
  io.openliberty.data.repository.function; type="ibm-api",\
  io.openliberty.data.repository.update; type="ibm-api",\
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
  jakarta.data.restrict; type="spec",\
  jakarta.data.spi; type="spec",\
  jakarta.data.spi.expression.function; type="spec",\
  jakarta.data.spi.expression.literal; type="spec",\
  jakarta.data.spi.expression.path; type="spec"
Subsystem-Name: Jakarta Data 1.1
-features=\
  com.ibm.websphere.appserver.eeCompatible-11.0,\
  io.openliberty.cdi-4.1,\
  io.openliberty.jakarta.data-1.1
-bundles=\
  io.openliberty.data; location:="dev/api/ibm/,lib/",\
  io.openliberty.data.internal,\
  io.openliberty.data.internal.beandef,\
  io.openliberty.data.1.1.internal
#  TODO add this back in if we GA this feature and include this IBM API
#  Also need to update io.openliberty.data/bnd.bnd file to move the bundle to dev/api/ibm
# -files=dev/api/ibm/javadoc/io.openliberty.data_1.0-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-12.0
