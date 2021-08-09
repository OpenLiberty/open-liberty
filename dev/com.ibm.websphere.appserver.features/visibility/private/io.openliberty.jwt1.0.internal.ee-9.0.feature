-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jwt_1.1-javadoc.zip
kind=beta
edition=core
