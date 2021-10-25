-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.appSecurityClient1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.api-5.0
-bundles=\
  io.openliberty.security.jaas.internal.common
-jars=io.openliberty.securityClient; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.securityClient_1.1-javadoc.zip
kind=beta
edition=base
