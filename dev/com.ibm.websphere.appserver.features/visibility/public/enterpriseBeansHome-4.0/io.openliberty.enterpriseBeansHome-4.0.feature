-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansHome-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeansHome-4.0
WLP-AlsoKnownAs: ejbHome-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0 Home Interfaces
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  io.openliberty.enterpriseBeansLite-4.0
-bundles=com.ibm.ws.ejbcontainer.ejb2x
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true; type:=beta
WLP-Platform: jakartaee-9.1,jakartaee-10.0,jakartaee-11.0
