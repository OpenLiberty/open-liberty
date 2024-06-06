-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpMetrics-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-1.3; ibm.tolerates:="1.4,2.0,2.1,2.2", \
    com.ibm.websphere.appserver.mpMetrics-1.1
kind=beta
edition=core
