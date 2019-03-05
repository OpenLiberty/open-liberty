-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidator-1.0
visibility=public
singleton=true
IBM-ShortName: configValidator-1.0
Subsystem-Name: Config and Validator REST API 1.0
-features=\
 com.ibm.websphere.appserver.mpOpenAPI-1.0,\
 com.ibm.websphere.appserver.restHandler-1.0,\
 com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="3.0, 4.0"
-bundles=\
 com.ibm.ws.rest.handler.config,\
 com.ibm.ws.rest.handler.validator,\
 com.ibm.ws.rest.handler.validator.jdbc,\
 com.ibm.ws.rest.handler.validator.openapi
kind=noship
edition=full
