-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.bouncycastle
WLP-DisableAllFeatures-OnConflict: false
-bundles= \
 io.openliberty.org.bouncycastle.bcpkix-jdk18on, \
 io.openliberty.org.bouncycastle.bcprov-jdk18on, \
 io.openliberty.org.bouncycastle.bcutil-jdk18on
kind=ga
edition=core
WLP-Activation-Type: parallel
