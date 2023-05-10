-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.nosql-1.0
visibility=public
singleton=true
IBM-ShortName: nosql-1.0
IBM-API-Package: \
  jakarta.nosql; type="spec",\
  jakarta.nosql.column; type="spec",\
  jakarta.nosql.document; type="spec",\
  jakarta.nosql.keyvalue; type="spec",\
Subsystem-Name: Jakarta NoSQL 1.0
#TODO com.ibm.websphere.appserver.eeCompatible-11.0
#TODO io.openliberty.jakartaeePlatform-11.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0,\
  io.openliberty.jakarta.nosql-1.0
-bundles=\
  io.openliberty.org.eclipse.jnosql.1.0,\
  io.openliberty.jakarta.data.1.0; location:="dev/api/spec/",\
  io.openliberty.jakarta.jsonb.3.0; location:="dev/api/spec/",\
  io.openliberty.org.eclipse.microprofile.config.3.0; location:="dev/api/stable/"
kind=noship
edition=full
WLP-Activation-Type: parallel
