# This "super-feature" will prefer to use the jsonp impl that includes Parsson
# However, this feature will tolerate the use of jsonp impl with user provided implementation
# This is important to avoid conflicts when intermixing the container / RI features of jsonb and jsonp
#   jsonb-3.1 > io.openliberty.jsonbInternal-3.1 > io.openliberty.jsonbImpl-3.1.1 > io.openliberty.jsonp-2.2 > io.openliberty.jsonpInternal-2.2
#   jsonpContainer-2.2 > io.openliberty.jsonpImpl-2.2.0
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jsonpInternal-2.2
visibility=private
singleton=true
-features=io.openliberty.jsonpImpl-2.2.1; ibm.tolerates:="2.2.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
