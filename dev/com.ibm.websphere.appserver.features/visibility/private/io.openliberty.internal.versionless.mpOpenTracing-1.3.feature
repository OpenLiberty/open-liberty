-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenTracing-1.3
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-2.2; ibm.tolerates:="3.0,3.2,3.3", \
    com.ibm.websphere.appserver.mpOpenTracing-1.3
kind=ga
edition=core
