-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.nosqlUnused-1.0
visibility=public
singleton=true
IBM-ShortName: nosqlUnused-1.0
Subsystem-Name: Unused feature to satisfy FeatureListValidator 1.0
# TODO remove this unused feature once we no longer need to transform the Jakarta NoSQL spec from javax packages that the FeatureListValidator expects to see used by a feature
-features=\
  com.ibm.websphere.appserver.eeCompatible-8.0,\
  com.ibm.websphere.appserver.javax.annotation-1.2,\
  com.ibm.websphere.appserver.javax.cdi-2.0,\
  com.ibm.websphere.appserver.javax.interceptor-1.2,\
  io.openliberty.noShip-1.0
-bundles=\
  com.ibm.websphere.javaee.jsonp.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.json:javax.json-api:1.1.3", \
  io.openliberty.javax.nosql.1.0; location:="dev/api/spec/,lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel
