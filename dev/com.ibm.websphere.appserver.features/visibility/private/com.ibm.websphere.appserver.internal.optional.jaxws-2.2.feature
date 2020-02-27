-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.optional.jaxws-2.2
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Java Web Services API 2.2
-bundles=\
 com.ibm.websphere.javaee.jaxws.2.2; require-java:="9"; location:="dev/api/spec/,lib/"; apiJar=false"
kind=ga
edition=core
WLP-Activation-Type: parallel
