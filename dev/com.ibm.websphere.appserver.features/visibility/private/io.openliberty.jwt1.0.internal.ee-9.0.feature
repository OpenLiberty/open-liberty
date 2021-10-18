-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
-jars=\
  io.openliberty.jwt; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/io.openliberty.jwt_1.1-javadoc.zip
kind=ga
edition=core
