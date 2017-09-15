# This private impl feature corresponds to jsonpContainer-1.1, which gives you
# JSON-P 1.1 spec with the ability to choose the default provider via a bell.
-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsonpImpl-1.1.0
singleton=true
visibility=private
-features=com.ibm.websphere.appserver.bells-1.0
-bundles=com.ibm.websphere.javaee.jsonp.1.1; location:="dev/api/spec/,lib/"
kind=beta
edition=core
