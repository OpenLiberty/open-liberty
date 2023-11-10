-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpMetrics-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-1.3; ibm.tolerates:="1.4,2.0,2.1,2.2", \
    com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
    com.ibm.websphere.appserver.mpMetrics-1.1
kind=beta
edition=core
