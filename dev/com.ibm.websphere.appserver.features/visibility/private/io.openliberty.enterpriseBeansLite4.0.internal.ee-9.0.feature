-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansLite4.0.internal.ee-9.0
singleton=true
visibility = private
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=core
WLP-Activation-Type: parallel
