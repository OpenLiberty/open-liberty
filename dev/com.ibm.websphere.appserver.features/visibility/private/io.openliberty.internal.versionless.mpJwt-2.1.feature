-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpJwt-2.1
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-6.0; ibm.tolerates:="6.1,7.0", \
    io.openliberty.mpJwt-2.1
kind=beta
edition=core
WLP-InstantOn-Enabled: true
