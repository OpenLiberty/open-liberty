-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenAPI-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.mpOpenAPI-2.0
kind=beta
edition=base
