-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.auditCollector-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-API-Package: com.ibm.websphere.security.audit; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.security.audit; type="ibm-spi"
IBM-Process-Types: server
-features=\
  com.ibm.websphere.appserver.restHandler-1.0,\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0, 5.0", \
  io.openliberty.auditCollector1.0.internal.ee-6.0; ibm.tolerates:="9.0", \
  com.ibm.websphere.appserver.wimcore-1.0

kind=ga
edition=core

