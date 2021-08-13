-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.el-3.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
-bundles= io.openliberty.el.internal.cdi, \
 com.ibm.websphere.javaee.el.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.el:javax.el-api:3.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
