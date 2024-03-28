-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpRestClient.internal.cdi-4.1
singleton=true
visibility=private
-features=io.openliberty.restfulWSClient-4.0,\
		  io.openliberty.noShip-1.0
-bundles=io.openliberty.org.jboss.resteasy.cdi.ee11
kind=noship
edition=full
WLP-Activation-Type: parallel
