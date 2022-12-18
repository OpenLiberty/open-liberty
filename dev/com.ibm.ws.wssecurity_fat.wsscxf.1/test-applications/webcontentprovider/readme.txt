***** webcontentclient and webcontentprovider are using the wsdl2java tool from CXF ******
I get a wsdl2java in apache-cxf-2.6.2\bin\wsdl2java (length 2,432)
and ran it as:
 wsdl2java  -frontend jaxws21 -all Echo.wsdl
it generates:
 com\ibm\was\cxfsample\sei\echo
           2,352 EchoService.java
           1,010 EchoServicePortType.java
           1,698 EchoServicePortTypeImpl.java
           1,977 EchoServicePortType_EchoServicePort_Client.java
             922 EchoServicePortType_EchoServicePort_Server.java
           1,555 EchoStringInput.java
           1,588 EchoStringResponse.java
           1,323 ObjectFactory.java
             131 package-info.java

I used these generated files (do some necessary modifications of course)
I am able to get it to run as a webServiceClient and a WebServiceProvider.
Please see the code in com.ibm.ws.wssecurity_fat 
     (I changed Echo.wsdl a little bit to add ws-addressing and a simple-username-token)
  test-applications\webcontentclient
  test-applications\webcontentprovider
  publish\servers\com.ibm.ws.wssecurity_fat.sample

************************** tcpmon log ************************************
... I love it tcpmon log.....

**********See SoapMessage sent from wsClient:
POST /webcontentprovider/EchoService HTTP/1.1
Content-Type: text/xml; charset=UTF-8
Accept: */*
SOAPAction: "echoOperation"
User-Agent: Apache CXF 2.6.2
Cache-Control: no-cache
Pragma: no-cache
Host: localhost
Connection: keep-alive
Content-Length: 1214

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Header>
      <Action xmlns="http://www.w3.org/2005/08/addressing">echoOperation</Action>
      <MessageID xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:d56ff5c8-1132-41a6-985a-a809a6659960</MessageID>
      <To xmlns="http://www.w3.org/2005/08/addressing">http://localhost:9085/webcontentprovider/EchoService</To>
      <ReplyTo xmlns="http://www.w3.org/2005/08/addressing">
         <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
      </ReplyTo>
      <wsse:Security soap:mustUnderstand="1" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
         <wsse:UsernameToken xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="UsernameToken-1">
            <wsse:Username>user1</wsse:Username>
            <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">security</wsse:Password>
            <wsu:Created>2013-03-05T22:56:43.968Z</wsu:Created>
         </wsse:UsernameToken>
      </wsse:Security>
   </soap:Header>
   <soap:Body>
      <a:echoStringInput xmlns:a="http://com/ibm/was/cxfsample/sei/echo/">
         <echoInput>Hello</echoInput>
      </a:echoStringInput>
   </soap:Body>
</soap:Envelope>


**********See SoapMessage responded from wsProvider:
HTTP/1.1 200 OK
X-Powered-By: Servlet/3.0
Content-Type: text/xml; charset=UTF-8
Content-Length: 738
Date: Tue, 05 Mar 2013 22:56:44 GMT
Server: WebSphere Application Server

<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Header>
      <Action xmlns="http://www.w3.org/2005/08/addressing">http://com/ibm/was/cxfsample/sei/echo/EchoServicePortType/echoOperationResponse</Action>
      <MessageID xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:a4904358-340a-4c5b-95f0-96b2db449840</MessageID>
      <To xmlns="http://www.w3.org/2005/08/addressing">http://www.w3.org/2005/08/addressing/anonymous</To>
      <RelatesTo xmlns="http://www.w3.org/2005/08/addressing">urn:uuid:d56ff5c8-1132-41a6-985a-a809a6659960</RelatesTo>
   </soap:Header>
   <soap:Body>
      <a:echoStringResponse xmlns:a="http://com/ibm/was/cxfsample/sei/echo/">
         <echoResponse>ID:user1:Hello</echoResponse>
      </a:echoStringResponse>
   </soap:Body>
</soap:Envelope>

