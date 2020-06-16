-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.jsp-3.0
singleton=true
-features=io.openliberty.jakarta.el-4.0; apiJar=false, \
 io.openliberty.jakarta.servlet-5.0; apiJar=false
-bundles=io.openliberty.jakarta.jsp.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
