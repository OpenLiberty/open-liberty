-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.servlet
visibility=public
IBM-ShortName: servlet
Subsystem-Name: Jakarta Servlet
-features=com.ibm.websphere.appserver.unversioned.servlet-0.0; ibm.tolerates:="3.1,4.0,5.0,6.0"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full