-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcache.internal1.1.ee-9.0
singleton=true
#
# CDI is optionally required by the JCache libraries.
#
-features=\
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0"
#  io.openliberty.jakarta.cdi-3.0; ibm.tolerates:="4.0, 4.1"
-bundles=\
  com.ibm.websphere.javaee.jcache.1.1.core.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
