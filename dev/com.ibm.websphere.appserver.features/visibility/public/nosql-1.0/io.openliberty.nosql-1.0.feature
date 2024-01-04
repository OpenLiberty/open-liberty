-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.nosql-1.0
visibility=public
singleton=true
IBM-ShortName: nosql-1.0
IBM-API-Package: \
  jakarta.nosql; type="spec",\
  jakarta.nosql.column; type="spec",\
  jakarta.nosql.document; type="spec",\
  jakarta.nosql.keyvalue; type="spec"
Subsystem-Name: Jakarta NoSQL 1.0
#TODO io.openliberty.jakartaeePlatform-11.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.jakarta.nosql-1.0

-bundles=\
  io.openliberty.java17.internal, \

kind=noship
edition=full
WLP-Activation-Type: parallel
