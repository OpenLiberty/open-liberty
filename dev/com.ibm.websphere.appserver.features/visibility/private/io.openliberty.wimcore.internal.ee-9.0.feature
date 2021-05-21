-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wimcore.internal.ee-9.0
Subsystem-Version: 9.0
singleton=true
visibility = private
-features=\
  io.openliberty.jakarta.xmlBinding-3.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.security.wim.internal.base
kind=noship
edition=full
