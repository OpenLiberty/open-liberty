-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurityClient-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package:\
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api", \
  com.ibm.websphere.security; type="ibm-api", \
  com.ibm.websphere.endpoint; type="ibm-api", \
  javax.rmi; type="spec", \
  javax.rmi.CORBA; type="spec", \
  org.omg.stub.java.rmi; type="spec", \
  org.omg.BiDirPolicy; type="spec", \
  org.omg.CONV_FRAME; type="spec", \
  org.omg.CORBA; type="spec", \
  org.omg.CORBA.ContainedPackage; type="spec", \
  org.omg.CORBA.ContainerPackage; type="spec", \
  org.omg.CORBA.InterfaceDefPackage; type="spec", \
  org.omg.CORBA.ORBPackage; type="spec", \
  org.omg.CORBA.PollableSetPackage; type="spec", \
  org.omg.CORBA.TypeCodePackage; type="spec", \
  org.omg.CORBA.ValueDefPackage; type="spec", \
  org.omg.CORBA.portable; type="spec", \
  org.omg.CORBA_2_3; type="spec", \
  org.omg.CORBA_2_3.portable; type="spec", \
  org.omg.CORBA_2_4; type="spec", \
  org.omg.CORBA_2_4.portable; type="spec", \
  org.omg.CSI; type="spec", \
  org.omg.CSIIOP; type="spec", \
  org.omg.CosNaming; type="spec", \
  org.omg.CosNaming.NamingContextExtPackage; type="spec", \
  org.omg.CosNaming.NamingContextPackage; type="spec", \
  org.omg.CosTSInteroperation; type="spec", \
  org.omg.CosTransactions; type="spec", \
  org.omg.Dynamic; type="spec", \
  org.omg.DynamicAny; type="spec", \
  org.omg.DynamicAny.DynAnyFactoryPackage; type="spec", \
  org.omg.DynamicAny.DynAnyPackage; type="spec", \
  org.omg.GIOP; type="spec", \
  org.omg.GSSUP; type="spec", \
  org.omg.IIOP; type="spec", \
  org.omg.IOP; type="spec", \
  org.omg.IOP.CodecFactoryPackage; type="spec", \
  org.omg.IOP.CodecPackage; type="spec", \
  org.omg.MessageRouting; type="spec", \
  org.omg.Messaging; type="spec", \
  org.omg.PortableInterceptor; type="spec", \
  org.omg.PortableInterceptor.ORBInitInfoPackage; type="spec", \
  org.omg.PortableServer; type="spec", \
  org.omg.PortableServer.CurrentPackage; type="spec", \
  org.omg.PortableServer.POAManagerFactoryPackage; type="spec", \
  org.omg.PortableServer.POAManagerPackage; type="spec", \
  org.omg.PortableServer.POAPackage; type="spec", \
  org.omg.PortableServer.ServantLocatorPackage; type="spec", \
  org.omg.PortableServer.portable; type="spec", \
  org.omg.SSLIOP; type="spec", \
  org.omg.Security; type="spec", \
  org.omg.SecurityLevel1; type="spec", \
  org.omg.SecurityLevel2; type="spec", \
  org.omg.SendingContext; type="spec", \
  org.omg.SendingContext.CodeBasePackage; type="spec", \
  org.omg.TimeBase; type="spec"
IBM-ShortName: appSecurityClient-1.0
Subsystem-Name: Application Security for Client 1.0
-features=io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  com.ibm.websphere.appserver.ssl-1.0, \
  com.ibm.websphere.appserver.csiv2Client-1.0, \
  io.openliberty.appSecurityClient1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  com.ibm.ws.security.authentication, \
  com.ibm.ws.security.credentials, \
  com.ibm.ws.security.token, \
  com.ibm.ws.security.authorization, \
  com.ibm.ws.security.client, \
  com.ibm.ws.security, \
  com.ibm.ws.security.registry, \
  com.ibm.websphere.security.impl, \
  com.ibm.ws.security.mp.jwt.proxy, \
  com.ibm.ws.security.token.s4u2
-jars=com.ibm.websphere.appserver.api.securityClient; location:=dev/api/ibm/, \
  io.openliberty.securityClient; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.securityClient_1.1-javadoc.zip, \
  dev/api/ibm/javadoc/io.openliberty.securityClient_1.1-javadoc.zip
kind=ga
edition=base
