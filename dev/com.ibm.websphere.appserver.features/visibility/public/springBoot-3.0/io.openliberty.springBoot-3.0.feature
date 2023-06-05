-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.springBoot-3.0
WLP-DisableAllFeatures-OnConflict: true
visibility=public
singleton=true
IBM-ShortName: springBoot-3.0
IBM-Process-Types: server
Subsystem-Name: Spring Boot Support 3.0
-features=io.openliberty.springBootHandler-3.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.java17.internal
kind=noship
edition=full
