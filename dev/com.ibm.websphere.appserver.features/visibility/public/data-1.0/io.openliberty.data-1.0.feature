-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data-1.0
visibility=public
singleton=true
IBM-ShortName: data-1.0
IBM-API-Package: \
  io.openliberty.data.repository; type="ibm-api",\
  io.openliberty.data.repository.comparison; type="ibm-api",\
  io.openliberty.data.repository.function; type="ibm-api",\
  io.openliberty.data.repository.update; type="ibm-api",\
  jakarta.data; type="spec",\
  jakarta.data.exceptions; type="spec",\
  jakarta.data.metamodel; type="spec",\
  jakarta.data.metamodel.impl; type="spec",\
  jakarta.data.page; type="spec",\
  jakarta.data.page.impl; type="spec",\
  jakarta.data.repository; type="spec"
Subsystem-Name: Jakarta Data 1.0
#TODO io.openliberty.jakartaeePlatform-11.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.cdi-4.0; ibm.tolerates:="4.1",\
  io.openliberty.jakarta.data-1.0
-bundles=\
  io.openliberty.data; location:="dev/api/ibm/,lib/",\
  io.openliberty.data.internal
-files=dev/api/ibm/javadoc/io.openliberty.data_1.0-javadoc.zip
kind=beta
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
