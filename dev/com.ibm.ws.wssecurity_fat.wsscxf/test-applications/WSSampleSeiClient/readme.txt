****************************
1) The Sample  tests are only in jaxws22 and only use the default SOAP version (soap11 or soap12...)
2) We only test Echo. And not on Ping or Async
3) The BSP wsdl policies. Liberty does not support this tag:
     <ns1:XPathFilter20  /> <!-- xmlns:ns1="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702" -->
4) For manual tests, you may want to turn off the FAT feature that will shutdown the server after 20-minute-inactive.
   To disable it. Mark this piece in <WLP-IMAGE>/usr/servers/fatTestPorts.xml
<server>
<!--
    <featureManager>
      <feature>timedexit-1.0</feature>
    </featureManager>
-->
 .....
</server>


***********************************************************************************************************************
***********************************************************************************************************************
The sample tests implemented the WSSecurity policies in:
https://infocenters.hursley.ibm.com/wlpsolo/vNext/draft/help/topic/com.ibm.websphere.wlp.nd.multiplatform.doc/ae/cwlp_wssec_templates.html

Echo1Service is equivalent to Scenario 1
Echo2Service is equivalent to Scenario 2
Echo3Service is equivalent to Scenario 3
Echo4Service is equivalent to Scenario 4
Echo5Service is equivalent to Scenario 5
Echo6Service is equivalent to Scenario 6
Echo7Service is equivalent to Scenario 7

BSP test
Echo11Service is equivalent to http://wsi20.rtp.raleigh.ibm.com:9080/BSP1/      - WSS-Basic128Sha15-EncHeader-ID-onHeader
Echo12Service is equivalent to http://wsi20.rtp.raleigh.ibm.com:9080/BSP2/      - WSS-Basic128Sha15-EncryptedHeader
Echo13Service is equivalent to http://wsi20.rtp.raleigh.ibm.com:9080/BSP3/      - WSS-Basic128Sha15-SignatureConfirmation
Echo14Service is equivalent to http://wsi20.rtp.raleigh.ibm.com:9080/BSP4/      - WSS-Def-Basic128Sha15-ForThumbprintRefScenario

Internal interop test X509:
Echo21Service is equivalent to WSSecurity Only. No wsaddressing (WSS1)
Echo22Service is equivalent to WSSecurity and wsaddressing      (WSS2)
Echo23Service is equivalent to WSSecurity and wsaddressing


*********************************************************************
If you need to test the ssl/https, you may want to:
1) A quick way to set up SSL is: copy the key.p12 and trust.p12 from <was-install-root>/profiles/AppSrv01/config/cells/<cell-name>/nodes/<node-name>
   to Liberty Server directory
2) And set up SSL settings in server.xml/server_bsp.xml:
    <sslDefault sslRef="DefaultSSLSettings" />
    <ssl id="DefaultSSLSettings"
         keyStoreRef="myKeyStore"
         trustStoreRef="myTrustStore"
         clientAuthenticationSupported="true" />
    <!-- ##for testing with SSL/https between WebSphere and Liberty
         ##You may want to copy the key.p12 and trust.p12 from WebSphere configuration over 
         ##here and using these settings for a quick test. -->    
    <keyStore id="myKeyStore" password="WebAS" type="PKCS12" location="${server.config.dir}/key.p12" />
    <keyStore id="myTrustStore" password="WebAS" type="PKCS12" location="${server.config.dir}/trust.p12" />

    

*********************************
*********************************
1) Change the Echo.wsdl in <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample/apps/WSSampleSeiClient/Echo.wsdl to match the testing policies.
2) Change the key files and server.xml in <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample to match the interop scenarios.
3) Change the Echo.wsdl in com.ibm.ws.wssecurity_fat/test-applications/WSSampleSei/resources/WEB-INF/wsdl/Echo.wsdl to change the policies or ServiceProvider.
   which will be in <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample/dropins/WSSampleSei.war (WEB-INF/wsdl/echo.wsdl)
4) For BSP, Change the EchoBsp.wsdl in com.ibm.ws.wssecurity_fat/test-applications/WSSampleSei/resources/WEB-INF/wsdl/EchoBsp.wsdl to change the policies or ServiceProvider.
   which will be in <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample/dropins/WSSampleSei.war (WEB-INF/wsdl/EchoBsp.wsdl)



***********************************************************************************************************************
***************************************************************************************************
If you have the RTC set up, you can see the sample source code in the following directories :
  com.ibm.ws.wssecurity_fat/fat/src/com/ibm/ws/wssecurity/fat/sample
  com.ibm.ws.wssecurity_fat/publish/servers/com.ibm.ws.wssecurity_fat.sample
  com.ibm.ws.wssecurity_fat/test-applications/WSSampleSeiClient
  com.ibm.ws.wssecurity_fat/test-applications/WSSampleSei


***********************************************************************************************************************
***************************************************************************************************
Examples of sending the EchoService to a different server and endpoint, such as: .ent:
With Echo1Service policy:
http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://wsi6.xxx.com:9080/net/EchoService&scenario=Echo1Service&test=echo&msgcount=1&options=soap11&message=Hello1

with Echo2Service policy:
http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://wsi6.xxx.com:9080/net/EchoService2&scenario=Echo2Service&test=echo&msgcount=1&options=soap11&message=Hello1

Basically, Parameter "scenario" decides which Policy to use. And Parameter "serviceURL" decides which endpoint to send the WebServiceRequest 


***********************************************************************************************************************
***************************************************************************************************
If you have the recent Liberty product Image installed:
1) zip up the server image of com.ibm.ws.wssecurity_fat.sample and unzip it at "<wlp-install-image-root>/usr" 
   such as: C:\jazz-eclipse\jazz\client\eclipse\workspace8\build.image\wlp\usr
2) See these instructions:
   *************** Using server_asym.xml(Asymmetric tests)****************
   1) In <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample 
         Such as: C:\jazz-eclipse\jazz\client\eclipse\workspace8\build.image\wlp\usr\servers\com.ibm.ws.wssecurity_fat.sample 
      Copy server_asym.xml to overwrite server.xml
   2) In <wlp-image-root>/bin do:
      server start com.ibm.ws.wssecurity_fat.sample
   3) Open an IE and submit the following endpoints plus parameters into the IE:

   ******Echo1Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://localhost:8020/WSSampleSei/Echo1Service&scenario=Echo1Service&test=echo&msgcount=1&options=soap11&message=Hello1

   **Expecting response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo1Service" status="pass" xmlns="http://www.wstf.org">Echo1SOAPImpl>>Hello1</testResult>

   ******Echo2Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://localhost:8020/WSSampleSei/Echo2Service&scenario=Echo2Service&test=echo&msgcount=1&options=soap11&message=Hello2

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo2Service" status="pass" xmlns="http://www.wstf.org">Echo2SOAPImpl>>Hello2</testResult>


   *****Echo4Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo4Service&scenario=Echo4Service&test=echo&msgcount=1&options=soap11&message=Hello4

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo4Service" status="pass" xmlns="http://www.wstf.org">Echo4SOAPImpl>>Hello4</testResult>


   *****Echo7Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo7Service&scenario=Echo7Service&test=echo&msgcount=1&options=soap11&message=Hello7

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo7Service" status="pass" xmlns="http://www.wstf.org">Echo7SOAPImpl>>Hello7</testResult>


***********************************************************************************************************************
*************** Using server_sha384.xml(X509 Symmetric)****************
1) do:
     server stop com.ibm.ws.wssecurity_fat.sample
   if you have not done so.
   *** I am not sure why, but I have to clean up before I start the Symmetric server
   *** Otherwise, it kept the server_asym.xml settings
2) In <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample 
   Copy server_sha384.xml to overwrite server.xml
3) In <wlp-image-root>/bin do:
   server start com.ibm.ws.wssecurity_fat.sample
4) Open an IE and submit the following endpoints plus parameters into the IE:

   ******Echo1Service (Yes, it duplicated with Asymmetric, since it's a UsernameToken test)
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://localhost:8020/WSSampleSei/Echo1Service&scenario=Echo1Service&test=echo&msgcount=1&options=soap11&message=Hello1

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo1Service" status="pass" xmlns="http://www.wstf.org">Echo1SOAPImpl>>Hello1</testResult>

   ******Echo2Service (Yes, it duplicated with Asymmetric, since it's a UsernameToken test) 
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=https://localhost:8020/WSSampleSei/Echo2Service&scenario=Echo2Service&test=echo&msgcount=1&options=soap11&message=Hello2

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo2Service" status="pass" xmlns="http://www.wstf.org">Echo2SOAPImpl>>Hello2</testResult>


   *****Echo3Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo3Service&scenario=Echo3Service&test=echo&msgcount=1&options=soap11&message=Hello3

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo3Service" status="pass" xmlns="http://www.wstf.org">Echo3SOAPImpl>>Hello3</testResult>



   *****Echo5Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo5Service&scenario=Echo5Service&test=echo&msgcount=1&options=soap11&message=Hello5

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo5Service" status="pass" xmlns="http://www.wstf.org">Echo5SOAPImpl>>Hello5</testResult>


   *****Echo6Service
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo6Service&scenario=Echo6Service&test=echo&msgcount=1&options=soap11&message=Hello6

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo6Service" status="pass" xmlns="http://www.wstf.org">Echo6SOAPImpl>>Hello6</testResult>


   *****Echo7Service (Yes, it duplicated with Asymmetric, since it's a UsernameToken test) 
   http://localhost:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://localhost:8010/WSSampleSei/Echo7Service&scenario=Echo7Service&test=echo&msgcount=1&options=soap11&message=Hello7

   **Expecting Response:
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo7Service" status="pass" xmlns="http://www.wstf.org">Echo7SOAPImpl>>Hello7</testResult>



***********************************************************************************************************************
*************** Using server_bsp.xml(Bsp 1-4 equivalent)****************
1) do:
     server stop com.ibm.ws.wssecurity_fat.sample
   if you have not done so.
   *** I am not sure why, but I have to clean up before I start the Symmetric server
   *** Otherwise, it kept the server_asym.xml settings
2) In <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample 
   Copy server_bsp.xml to overwrite server.xml
3) In <wlp-image-root>/bin do:
   server start com.ibm.ws.wssecurity_fat.sample
   (Change the harmonic.austin.ibm.com into the hostname you are testing. )
4) Open an IE and submit the following endpoints plus parameters into the IE:

   ******Echo11Service (http://wsi20.rtp.raleigh.ibm.com:9080/BSP1/     - WSS-Basic128Sha15-EncHeader-ID-onHeader)
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo11Service&scenario=Echo11Service&test=echo&msgcount=1&options=soap11&message=Hello11

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo11Service" status="pass" xmlns="http://www.wstf.org">Echo11SOAPImpl>>Hello11</testResult>

   ******Echo12Service (http://wsi20.rtp.raleigh.ibm.com:9080/BSP2/      - WSS-Basic128Sha15-EncryptedHeader )
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo12Service&scenario=Echo12Service&test=echo&msgcount=1&options=soap11&message=Hello12

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo12Service" status="pass" xmlns="http://www.wstf.org">Echo12SOAPImpl>>Hello12</testResult>

   ******Echo13Service  (http://wsi20.rtp.raleigh.ibm.com:9080/BSP3/      - WSS-Basic128Sha15-SignatureConfirmation )
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo13Service&scenario=Echo13Service&test=echo&msgcount=1&options=soap11&message=Hello13

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo13Service" status="pass" xmlns="http://www.wstf.org">Echo13SOAPImpl>>Hello13</testResult>

   ******Echo14Service (http://wsi20.rtp.raleigh.ibm.com:9080/BSP4/      - WSS-Def-Basic128Sha15-ForThumbprintRefScenario )
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo14Service&scenario=Echo14Service&test=echo&msgcount=1&options=soap11&message=Hello14

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo14Service" status="pass" xmlns="http://www.wstf.org">Echo14SOAPImpl>>Hello14</testResult>


***********************************************************************************************************************
*************** Using server_x509.xml(internal interop x509)****************
1) do:
     server stop com.ibm.ws.wssecurity_fat.sample
   if you have not done so.
   *** I am not sure why, but I have to clean up before I start the Symmetric server
   *** Otherwise, it kept the server_asym.xml settings
2) In <wlp-image-root>/usr/servers/com.ibm.ws.wssecurity_fat.sample 
   Copy server_x509.xml to overwrite server.xml
3) In <wlp-image-root>/bin do:
   server start com.ibm.ws.wssecurity_fat.sample
   (Change the harmonic.austin.ibm.com into the hostname you are testing. )
4) Open an IE and submit the following endpoints plus parameters into the IE:

   ******Echo21Service (http://wsi20.rtp.raleigh.ibm.com:9080/BSP1/     - WSS-Basic128Sha15-EncHeader-ID-onHeader)
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo21Service&scenario=Echo21Service&test=echo&msgcount=1&options=soap11&message=Hello11

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo21Service" status="pass" xmlns="http://www.wstf.org">Echo21SOAPImpl>>Hello11</testResult>

   ******Echo22Service (http://wsi20.rtp.raleigh.ibm.com:9080/BSP2/      - WSS-Basic128Sha15-EncryptedHeader )
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo22Service&scenario=Echo22Service&test=echo&msgcount=1&options=soap11&message=Hello12

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo22Service" status="pass" xmlns="http://www.wstf.org">Echo22SOAPImpl>>Hello12</testResult>

   ******Echo23Service  (http://wsi20.rtp.raleigh.ibm.com:9080/BSP3/      - WSS-Basic128Sha15-SignatureConfirmation )
   http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://harmonic.austin.ibm.com:8010/WSSampleSei/Echo23Service&scenario=Echo23Service&test=echo&msgcount=1&options=soap11&message=Hello13

   **Expecting response
   <?xml version="1.0"?>
   <testResult options="soap11" test="echo" scenario="Echo23Service" status="pass" xmlns="http://www.wstf.org">Echo23SOAPImpl>>Hello13</testResult>

*********************************
For testing example toward tWas or .net:
.net WSS1
  http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://wsi6.rtp.raleigh.ibm.com:9094/WSSampleSei/EchoService&scenario=Echo21Service&test=echo&msgcount=1&options=soap11&message=Hello.net.9094Direct
.net WSS2                                          
  http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://wsi6.rtp.raleigh.ibm.com:9095/WSSampleSei/EchoService&scenario=Echo22Service&test=echo&msgcount=1&options=soap11&message=Hello.net.9095AddressingDirect

tWas WSS1
  http://harmonic.austin.ibm.com:8010/WSSampleSeiClient/ClientServlet?serviceURL=http://wsi20.rtp.raleigh.ibm.com:9080/WSSampleSei/EchoService&scenario=Echo21Service&test=echo&msgcount=1&options=soap11&message=HelloWSS19080Direct

