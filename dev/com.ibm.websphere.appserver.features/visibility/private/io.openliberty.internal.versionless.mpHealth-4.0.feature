-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpHealth-4.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-5.0; ibm.tolerates:="6.0,6.1,7.0", \
    io.openliberty.mpHealth-4.0
kind=beta
edition=core
WLP-InstantOn-Enabled: true
