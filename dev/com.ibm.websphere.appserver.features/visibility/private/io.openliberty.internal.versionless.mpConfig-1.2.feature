-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpConfig-1.2
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-1.3; ibm.tolerates:="1.4,2.0,2.1,2.2,3.0,3.2,3.3", \
    com.ibm.websphere.appserver.mpConfig-1.2
kind=beta
edition=core
