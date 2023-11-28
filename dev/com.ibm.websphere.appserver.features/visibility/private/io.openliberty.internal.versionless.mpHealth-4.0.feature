-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpHealth-4.0
visibility=private
singleton=true
-features= \
    com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="9.0", \
    io.openliberty.mpHealth-4.0
kind=beta
edition=core
