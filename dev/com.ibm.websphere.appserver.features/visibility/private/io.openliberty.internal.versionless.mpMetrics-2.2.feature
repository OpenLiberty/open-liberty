-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpMetrics-2.2
visibility=private
singleton=true
-features= \
    com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
    com.ibm.websphere.appserver.mpMetrics-2.2
kind=beta
edition=core
