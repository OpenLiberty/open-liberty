-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakartaMail-1.6
visibility=public
singleton=true
IBM-ShortName: jakartaaMail-1.6
Subsystem-Version: 1.6
Subsystem-Name: JakartaMail 1.6
IBM-API-Package: \
 javax.mail; type="spec", \
 javax.mail.internet; type="spec", \
 javax.mail.util; type="spec", \
 javax.mail.search; type="spec", \
 javax.mail.event; type="spec"
-bundles=\
  com.ibm.websphere.jakarta.activation.2.0; require-java:="9"; location:="dev/api/spec/,lib/"; apiJar=false
kind=noship
edition=core
