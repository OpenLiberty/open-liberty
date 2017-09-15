-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonp-1.0
visibility=public
IBM-API-Package: javax.json; type="spec", \
 javax.json.stream; type="spec", \
 javax.json.spi; type="spec"
IBM-ShortName: jsonp-1.0
Subsystem-Name: JavaScript Object Notation Processing
-bundles=com.ibm.websphere.javaee.jsonp.1.0; location:="dev/api/spec/,lib/", \
 com.ibm.ws.org.glassfish.json.1.0
kind=ga
edition=core
