-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpMetrics-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.eeCompatible-8.0, \
    com.ibm.websphere.appserver.mpMetrics-3.0
kind=beta
edition=core
