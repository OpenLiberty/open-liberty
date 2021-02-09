/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.x509migtoken;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.crypto.Cipher;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
//Added 11/2020
import org.junit.runner.RunWith;

//Added 11/2020
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
//Added 11/2020
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

//Added 11/2020
@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfX509MigSymTests {

    //orig from CL:
    //private static String serverName = "com.ibm.ws.wssecurity_fat.x509migsym";
    //private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

    //Added 11/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509migsym";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfX509MigSymTests.class;

    static boolean debugOnHttp = true;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String x509MigSymClientUrl = "";
    private static String x509MigBadSymClientUrl = "";
    private String methodFull = null;

    static String hostName = "localhost";

    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        copyServerXml(System.getProperty("user.dir") + File.separator + server.getPathToAutoFVTNamedServer() + "server_sha512.xml");

        String thisMethod = "setup";

        //orig from CL
        //SharedTools.installCallbackHandler(server);

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "x509migclient", "com.ibm.ws.wssecurity.fat.x509migclient", "test.libertyfat.x509mig.contract", "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migbadclient", "com.ibm.ws.wssecurity.fat.x509migbadclient", "test.libertyfat.x509mig.contract",
                                      "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migtoken", "basicplcy.wssecfvt.test");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");

        server.startServer(); // check CWWKS0008I: The security service is ready.
        SharedTools.waitForMessageInLog(server, "CWWKS0008I");
        portNumber = "" + server.getHttpDefaultPort();
        portNumberSecure = "" + server.getHttpDefaultSecurePort();

        server.waitForStringInLog("port " + portNumber);
        server.waitForStringInLog("port " + portNumberSecure);
        // check  message.log
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now lis....Port 8010
        assertNotNull("defaultHttpendpoint may not started at :" + portNumber,
                      server.waitForStringInLog("CWWKO0219I.*" + portNumber));
        // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now lis....Port 8020
        assertNotNull("defaultHttpEndpoint SSL port may not be started at:" + portNumberSecure,
                      server.waitForStringInLog("CWWKO0219I.*" + portNumberSecure));

        // using the original port to send the parameters
        x509MigSymClientUrl = "http://localhost:" + portNumber +
                              "/x509migclient/CxfX509MigSvcClient";
        // using the original port to send the parameters
        x509MigBadSymClientUrl = "http://localhost:" + portNumber +
                                 "/x509migbadclient/CxfX509MigBadSvcClient";
        // portNumber = "9085";                // for debugging
        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        return;

    }

    /**
     * TestDescription:
     *
     * Test KeyIdentifierPolicy.
     * <!-- bax01 -->
     * <wsp:Policy wsu:Id="X509KeyIdentifierPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireKeyIdentifierReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-1">
     * <wsu:Created>
     * 2012-12-20T19:14:18.548Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:18.548Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B3613560308585851">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier">
     * /SI03jIK6G7W4H1pmoW3Jf9rIUA=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * Evyl6c8T0fRjg6Jrn98UVCVJw4sRBqzklyn/sAy6idG/LlKcfnKW5bL3ZL1ckBp9D0f2k/pOQIFKWIHOwhhvcBd2YRwCOgYL6ldvN6QHMao0292eSiUrfIGCN5xS8u+wOhoOhxE+CvVNiNM4vreUqS2gC+RXYpnN5g7ys4uHzhc=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-3"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-2">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-883980723">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * RWKJl80CHb4DQQG7sqJoaJlzBOs=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-1">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * WdU3Q3lrMkpnjl05UkMJZMWqIIg=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * EkRSa73B6timSQ+Q1CeQ5J2FZmc=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B3613560308586012">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B3613560308586023">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B3613560308585851"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-883980723">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-3"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B3613560308585851"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * QTUrcL+Gd6Gshh3OR//unXTHkZWeh+dhUVb+2OJMAhaFP4UqeOkos+5jLblz03GZPM9skK2A5CY4fIYK74S5O50gMGJg4FZ47UAFBXZobsv25bNYPinBDPXG/s5uzB8W
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     *
     */

    @Test
    @AllowedFFDC(value = { "java.lang.Exception" })
    public void testCxfX509KeyIdMigSymService() throws Exception {

        String thisMethod = "testCxfX509KeyIdMigSymService";
        methodFull = "testCxfX509KeyIdMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX01Service", //String strServiceName,
                        "UrnX509Token01" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Same as the testCxfX509KeyIdMigSymService but this goes with https
     *
     */

    @Test
    @AllowedFFDC(value = { "java.lang.Exception" })
    public void testCxfX509KeyIdMigSymServiceHttps() throws Exception {
        String thisMethod = "testCxfX509KeyIdMigSymService";
        methodFull = "testCxfX509KeyIdMigSymServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX01Service", //String strServiceName,
                        "UrnX509Token01" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * test Symmetric IssuerSerialPolicy
     * <!-- bax03 -->
     * <wsp:Policy wsu:Id="X509IssuerSerialPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token11 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-13">
     * <wsu:Created>
     * 2012-12-20T19:14:19.398Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:19.398Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603085939911">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <ds:X509Data>
     * <ds:X509IssuerSerial>
     * <ds:X509IssuerName>
     * CN=test2,O=IBM,C=US
     * </ds:X509IssuerName>
     * <ds:X509SerialNumber>
     * 1353451350
     * </ds:X509SerialNumber>
     * </ds:X509IssuerSerial>
     * </ds:X509Data>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * fN1lg3I7x5nkjNRz5/AkjAtOFt5k68xS0mqjyEqWtENa1S1AB16/V4OcSfkJ17DfAgsjtBIpkixyDpjI5OrcHnkKRXGdhYLCtekfPFr3b/GSwItGUMDhTeUYYG9MSk3GoYCHxrfp8oXe4DTGzxvOeZJHj0A4ddqMYv9o6+ojIFU=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-15"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-14">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-400485205">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * LMSuBvQO/J7NL6yvg4Mk6BZFUpo=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-13">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * 5Zpj0Wq66jdwnWtBCUWfkUaLyFM=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * jwqEKYXCVQmCHcr6DXHSPvSFeQA=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603085940212">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603085940213">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085939911"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-400485205">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-15"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085939911"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * VWCPyoiyOL1SQDSYVznP3RbZXL7LFNIlIEQFrkt5aiB/usGHm1zp+Hj2/A23pt68+iGF6tIlIu0tE0VHcNqtb+LLBYtWvjBmJpCQLzVJOmKgrnfwLxPAD/3KrEL4/NF4eVDGXE4Bicl96MN7beIBMg==
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509IssuerSerialMigSymService() throws Exception {
        String thisMethod = "testCxfX509IssuerSerialMigSymService";
        methodFull = "testCxfX509IssuerSerialMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509IssuerSerialPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX03Service", //String strServiceName,
                        "UrnX509Token03" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * test SymmetricProtectTokensPolicy
     * <!-- bax09 -->
     * <wsp:Policy wsu:Id="X509SymmetricProtectTokensPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token11 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:ProtectTokens/>
     * <sp:SignBeforeEncrypting/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="EEF851A0D5D1683B36135603085959817">
     * MIIBzTCCATagAwIBAgIEUKwHVjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MjAeFw0xMjExMjAyMjQyMzBaFw0zNDEwMTYyMjQyMzBaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QyMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDPWOQKPX8I3qzsZdcKWHxFZC1a
     * +yysPEZpB5ZqlHc0ZKxfj/20k+1O6Gu8C7yqY2ZqkZSSyKVHWEhcUMZ38r+9Ib664YNFTzYxvNLqFGZW8+79h5Jb8h+jQ8/iN0MuMEevz0LpzPsbyG+mNKyg/
     * dhiVh6uhd5SiGm5EJTjcz7Q9QIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAAnykqGOEjrrWFyfK7ULICLnMQ3hqwcvyj29LpbkrBHJ1Ko7M7ptlz1Vx0teoTdMv04WkOeBhg5y0SqtWxPmrfsrkBC6pYfJZoDr26DIT5ZfMJE
     * +NrKWBM4NrfyTtIitlT2Lyb5zhJYFD/7n0UdsXGLRAdlOsODlvHrvS8z1MgPA
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-19">
     * <wsu:Created>
     * 2012-12-20T19:14:19.596Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:19.596Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603085959816">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:Reference URI="#EEF851A0D5D1683B36135603085959817"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * JQOyhOUSesJzIdikTcku2+SN91dLILbpScIdRgEF4i0iCV2DC741SqLM59MUfbLOngGyZGUurYbJwCs01PXixTAnPoyDZEV/4Va4h6GRUoPjCEftQEYkQaIcXV7wV0RN8Ws3R8tO8eXL7YSwl1tuU2zpoxBfSNe4yKFJn9ke1fw=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-21"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-20">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id--239997262">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * JF2UaMK35gUGAwCJALtVQKeIHns=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-19">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * PvyvciVLM0iyFj30PGK5JCquRTQ=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#EK-EEF851A0D5D1683B36135603085959816">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse wsu soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * e5ilawvw4u3RpVxZPu09Vug7OIM=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * sKXpUD6xiZGO42uVv9lnaFwWi0M=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603085960018">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603085960019">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085959816"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--239997262">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-21"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085959816"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * UKY/SvRAF4EJt0/qUzsCtFVayvx+vmz6B8L+EAywSvVvqIPNK3g+CKLwvwwTWyY3qFHViFW4se4HuV3LzIC32iuwCyF6IAzWdYHZcpKVQxJLj/xJTM61nkNDyQlruiJPIiPg9TkRIIvOZTGY320JSHdD9n39IF3BqZR6SnNRVlw=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     *
     */

    @Test
    public void testCxfX509ProtectTokensMigSymService() throws Exception {
        String thisMethod = "testCxfX509ProtectTokensMigSymService";
        methodFull = "testCxfX509ProtectTokensMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509SymmetricProtectTokensPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX09Service", //String strServiceName,
                        "UrnX509Token09" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test Symmetric ThumbprintPolicy
     * <!-- bax04 -->
     * <wsp:Policy wsu:Id="X509ThumbprintPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token11 />
     * <sp:RequireThumbprintReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:SignBeforeEncrypting/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-25">
     * <wsu:Created>
     * 2012-12-20T19:14:19.844Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:19.844Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603085984622">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1">
     * g0pNyKdzNr5oceeQwE2Ema44cE8=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * X83VDaYwtshTiXcWc7DPRX9WwN5Ml0Z37Uw5/oyWiXWa0ndYQBcdf986WEbAt1DJb7fs5x8/ZWhuNTrFnC11Ex7wzWWEr+hXO73xqnkvjeC8/S/lPae79QApVnAF6dA+xfTmQb4mPKjX72cuxBqOvsrAfHkaWj6Df8z9KBz4LKk=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-27"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-26">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-1918186578">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * oAzJZi+C4cu8s+v5DzpxbOkNs58=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-25">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * sMMzQ4t0al0usCu4q91Y0uNwbNM=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * GNgsQGCr+KSjhc6UJ7wIGRhzj6I=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603085984723">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603085984724">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085984622"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-1918186578">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-27"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603085984622"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * H+PwKPVEnZvpTPfUnT42gHTgHU9+Y6gVr+OWpso+gvo9EOIqgn3NU4PAxWFCA6s8QsOoHN0UMqGXJ5QE1N9wvLE3i/VvUA8WPSlLXSY3DDjOGwIsCRnUnyfMkSWdcjfJBnEDlcXhWZtLrVMM7mlSRw==
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509ThumbprintMigSymService() throws Exception {
        String thisMethod = "testCxfX509ThumbprintMigSymService";
        methodFull = "testCxfX509ThumbprintMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509ThumbprintPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX04Service", //String strServiceName,
                        "UrnX509Token04" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test symmetric KeyIdentifierDerivedPolicy
     * <!-- bax05 -->
     * <wsp:Policy wsu:Id="X509KeyIdentifierDerivedPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireKeyIdentifierReference />
     * <sp:RequireDerivedKeys/>
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <!-- sp:ProtectTokens/> <newly added GKUO -->
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-31">
     * <wsu:Created>
     * 2012-12-20T19:14:20.048Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:20.048Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603086005127">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier">
     * /SI03jIK6G7W4H1pmoW3Jf9rIUA=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * vN/DTkseCDThLxPqhFadta56I0eKjIaW7pWwOA5sinSL+SsP9Vt+EELN5x+ga3a+Z0atHEuxEqYdbyyXnvl6k+VMgrcariESZItih/rOpPPVFYSBAoqv0jOxa/DAXnMXh7FyfSkgRx8s+BKjVBPm1IEK2btNoqySacq/AdURHrE=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <wsc:DerivedKeyToken xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"
     * wsu:Id="DK-32">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603086005828">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086005127"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * <wsc:Offset>
     * 0
     * </wsc:Offset>
     * <wsc:Length>
     * 16
     * </wsc:Length>
     * <wsc:Nonce>
     * BIMAxY1XltW9oQCD9bkCXw==
     * </wsc:Nonce>
     * </wsc:DerivedKeyToken>
     * <wsc:DerivedKeyToken xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"
     * wsu:Id="DK-34">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603086006231">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086005127"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * <wsc:Offset>
     * 0
     * </wsc:Offset>
     * <wsc:Length>
     * 16
     * </wsc:Length>
     * <wsc:Nonce>
     * 0EIo6C6q9YMqniA6b6Lw9w==
     * </wsc:Nonce>
     * </wsc:DerivedKeyToken>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-35"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-33">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-185355174">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * MwRIf2pG2QTW6XSDSMyurVINKNM=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-31">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * N762pAOiNPu6IiU9tIYo6MseuhA=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * oFII86lb3Z/Vd1WTVrMDkJAsJXU=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603086005829">
     * <wsse:SecurityTokenReference wsu:Id="STR-EEF851A0D5D1683B36135603086005830">
     * <wsse:Reference URI="#DK-32"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-185355174">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-35"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:Reference URI="#DK-34"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * FpKEYr2xoYO0Lz+T+zXC2JU+4ISoaiQVDsG5jvLaWrNrkPCMmQTYQSohAuJIFEM6PP9XJcz46QNgF5Dp8ahX1fjv1emEeFx8uSKjqhKAuYl+KEHOywnhdoepuPJ1jATijT92OrR8/lwh5W3pAyZAWg==
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509KeyIdDerivedMigSymService() throws Exception {
        String thisMethod = "testCxfX509KeyIdDerivedMigSymService";
        methodFull = "testCxfX509KeyIdDerivedMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierDerivedPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX05Service", //String strServiceName,
                        "UrnX509Token05" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test Symmetric ThumbprintDerivedPolicy
     * <!-- bax06 -->
     * <wsp:Policy wsu:Id="X509ThumbprintDerivedPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:SymmetricBinding>
     * <wsp:Policy>
     * <sp:ProtectionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token11 />
     * <sp:RequireThumbprintReference />
     * <sp:RequireDerivedKeys/>
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:ProtectionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:SignBeforeEncrypting/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:SymmetricBinding>
     * <sp:EncryptedParts>
     * <sp:Body/>
     * </sp:EncryptedParts>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-41">
     * <wsu:Created>
     * 2012-12-20T19:14:20.274Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:20.274Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603086027734">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1">
     * g0pNyKdzNr5oceeQwE2Ema44cE8=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * m2rqcgj1aOe8E+3bmP0mhmWhntVcukj44pkx61zYJDXZPlvIU46xklmXWx0+Td+i0l0jCiJY+H9LTOWXn57YBa1PW7l6PCUmK4qjZbkmx7vJIPNuk2a9D66EbiRCoFWLiR0C83l6AIFBTBelkRUblltFd8EotpMngYk9x4VxEUw=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <wsc:DerivedKeyToken xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"
     * wsu:Id="DK-42">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603086028035">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086027734"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * <wsc:Offset>
     * 0
     * </wsc:Offset>
     * <wsc:Length>
     * 16
     * </wsc:Length>
     * <wsc:Nonce>
     * Yex6mRBBhw1fZORkJZfHgQ==
     * </wsc:Nonce>
     * </wsc:DerivedKeyToken>
     * <wsc:DerivedKeyToken xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512"
     * wsu:Id="DK-44">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603086028238">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086027734"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * <wsc:Offset>
     * 0
     * </wsc:Offset>
     * <wsc:Length>
     * 16
     * </wsc:Length>
     * <wsc:Nonce>
     * /7uus++Qg+PFjArDjUpwcQ==
     * </wsc:Nonce>
     * </wsc:DerivedKeyToken>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-45"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-43">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id--1856303691">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * U+iq5BUuUApPUevG0dmJDF0A+h4=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-41">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * i+178/3xS4UmQupQ8KYa9A+l4Ic=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * dfjgxdhTfUEnBeQ9xfqfxivkEgc=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603086028036">
     * <wsse:SecurityTokenReference wsu:Id="STR-EEF851A0D5D1683B36135603086028037">
     * <wsse:Reference URI="#DK-42"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--1856303691">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-45"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
     * <wsse:Reference URI="#DK-44"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * P65vHWw0g2cWkYi4UPgrZdhW6yepcP3YaT0KIj4oJyLmarLwjrRCpuovhWIvE58P42Tq915tlyKnNxYb/onvBZAFBmEaGzqnwZEtGyXiFlsby1XdHxadPKMmtDNCP87BspRaIeZCtY68MJufCp5qRA==
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509ThumbprintDerivedMigSymService() throws Exception {
        String thisMethod = "testCxfX509ThumbprintDerivedMigSymService";
        methodFull = "testCxfX509ThumbprintDerivedMigSymService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509ThumbprintDerivedPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX06Service", //String strServiceName,
                        "UrnX509Token06" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * The test case test:
     * <sp:EncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp13:Created/>
     * <sp13:Nonce/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:EncryptedSupportingTokens>
     * and make sure it works
     *
     **/
    @Test
    public void testX509KeyIdentifierUNTService() throws Exception {
        String thisMethod = "testX509KeyIdentifierUNTService";
        methodFull = "testX509KeyIdentifierUNTService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509KeyIdentifierUNTPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX24Service", //String strServiceName,
                        "UrnX509Token24" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * The test case test:
     * <sp:SignedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp13:Created/>
     * <sp13:Nonce/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:SignedSupportingTokens>
     * and also
     * <sp:RequireSignatureConfirmation/>
     * and make sure it works
     *
     **/
    @Test
    public void testX509SignatureConfirmService() throws Exception {
        String thisMethod = "testX509SignatureConfirmService";
        methodFull = "testX509SignatureConfirmService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Sym_SignConfirmPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX25Service", //String strServiceName,
                        "UrnX509Token25" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This tests:
     * <sp:SignedSupportingTokens
     * xmlns:sp="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702">
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssUsernameToken10 />
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:SignedSupportingTokens>
     * It expects to pass
     **/
    @Test
    public void testSymEncSignService() throws Exception {
        String thisMethod = "testSymEncSignService";
        methodFull = "testSymEncSignService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymEncSignPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX27Service", //String strServiceName,
                        "UrnX509Token27" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This test provide The policy layout
     * We switch the Strict and Lax between the Service Provider and Client Requester
     * ** See the testBadEncSignStrictService as well
     * Strict and Lax does not matter.
     * But we do not know if it's ignored or they just follow Strict or Lax
     * In case, this test case, we need to figure out and maybe document
     *
     **/
    @Test
    public void testSymEncSignStrictService() throws Exception {
        String thisMethod = "testSymEncSignStrictService";
        methodFull = "testSymEncSignStrictService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymEncSignStrictPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX29Service", //String strServiceName,
                        "UrnX509Token29" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     * using Basic192
     * It's a negative test unless we put in the UnrestrictedPolicySecurityPolicy
     * Once it's implemented, the test become positive and test in here is set to negative on regular tests
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBasic192Service() throws Exception {
        String thisMethod = "testBasic192Service";
        methodFull = "testBasic192Service";

        // According the KC: IBM® SDK, on all platforms, provides unlimited jurisdiction policy files.
        // However this wasn't always the case, earlier SDKs the default is limited which means that
        // this test will fail on the SDKs that don't have unlimited set by default.
        if ((Cipher.getMaxAllowedKeyLength("AES") >= 192) == true) {
            try {
                testRoutine(
                            thisMethod, //String thisMethod,
                            "SymEncSignBasic192Policy", // Testing policy name
                            "negative", // Positive, positive-1, negative or negative-1... etc
                            portNumber, //String portNumber,
                            "", //String portNumberSecure
                            "FatBAX31Service", //String strServiceName,
                            "UrnX509Token31" //String strServicePort
                );
            } catch (Exception e) {
                throw e;
            }
        }

        return;
    }

    // This proves that InclusiveC14N is in troubles.
    // With InclusiveC14N
    //   When we specify Basic128Sha256, this test fail
    //   But when we specify Basic128, the test passed.
    // Checked, the InclusiveC14N is not built in Client. It's
    //    .......
    //         <ds:Reference URI="#UsernameToken-100">
    //            <ds:Transforms>
    //               <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
    //                  <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="soapenv"></ec:InclusiveNamespaces>
    //               </ds:Transform>
    //            </ds:Transforms>
    //            <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"></ds:DigestMethod>
    //            <ds:DigestValue>3Uk8HpK4bUmsCpD6sHyOCiis4XM=</ds:DigestValue>
    //         </ds:Reference>
    //      </ds:SignedInfo>
    //  in tcpmon
    //@Test
    //@AllowedFFDC("org.apache.ws.security.WSSecurityException")
    //@AllowedFFDC("javax.net.ssl.SSLException")
    public void testInclusiveC14NService() throws Exception {
        String thisMethod = "testInclusiveC14NService";
        methodFull = "testInclusiveC14NService";

        boolean debug = false;
        String tmpPort = portNumber;
        try {
            if (debug) {
                portNumber = "9085";
            }
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymEncSignInclusiveC14NPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX33Service", //String strServiceName,
                        "UrnX509Token33" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            if (debug) {
                portNumber = tmpPort;
            }
        }

        return;
    }

    //
    //  This tests the
    //
    // <sp:EndorsingSupportingTokens xmlns:sp="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702">
    //    <wsp:Policy>
    //         <sp:X509Token
    //            sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
    //            <wsp:Policy>
    //               <sp:RequireThumbprintReference/>
    //               <sp:WssX509V3Token10/>
    //            </wsp:Policy>
    //         </sp:X509Token>
    //     </wsp:Policy>
    // </sp:EndorsingSupportingTokens>
    // With SymmetricBinding
    //
    @Test
    @AllowedFFDC("javax.net.ssl.SSLException")
    public void testSymmetricEndorsingUNTPolicy() throws Exception {
        String thisMethod = "testSymmetricEndorsingUNTPolicy";
        methodFull = "testSymmetricEndorsingUNTPolicy";

        boolean debug = false;
        String tmpPort = portNumber;
        try {
            if (debug) {
                portNumber = "9085";
            }
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymmetricEndorsingUNTPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX35Service", //String strServiceName,
                        "UrnX509Token35" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            if (debug) {
                portNumber = tmpPort;
            }
        }

        return;
    }

    @Test
    public void testSymmetricEndorsingUNTPolicyHttps() throws Exception {
        String thisMethod = "testSymmetricEndorsingUNTPolicy";
        methodFull = "testSymmetricEndorsingUNTPolicyHttps";

        boolean debug = false;
        String tmpPort = portNumber;
        try {
            if (debug) {
                portNumber = "9085";
            }
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymmetricEndorsingUNTPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX35Service", //String strServiceName,
                        "UrnX509Token35" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            if (debug) {
                portNumber = tmpPort;
            }
        }

        return;
    }

    //
    //  This tests the
    //
    // <sp:SymmetricBinding>
    //     <wsp:Policy>
    //         <sp:EncryptionToken>
    //             <wsp:Policy>
    //                 <sp:X509Token sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
    //                     <wsp:Policy>
    //                         <sp:WssX509V3Token10/>
    //                     </wsp:Policy>
    //                 </sp:X509Token>
    //             </wsp:Policy>
    //         </sp:EncryptionToken>
    //         <sp:SignatureToken>
    //             <wsp:Policy>
    //                 <sp:X509Token sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
    //                     <wsp:Policy>
    //                         <sp:WssX509V3Token10/>
    //                     </wsp:Policy>
    //                 </sp:X509Token>
    //             </wsp:Policy>
    //         </sp:SignatureToken>
    //         ....
    //     </wsp:Policy>
    // </sp:SymmetricBinding>
    // With SymmetricBinding
    //
    // @Test
    // This test on SignatureToken and EncryptionToken does not work
    public void testSymSignatureTokenPolicy() throws Exception {
        String thisMethod = "testSymSignatureTokenPolicy";
        methodFull = "testSymSignatureTokenPolicy";

        boolean debug = false;
        String tmpPort = portNumber;
        try {
            if (debug) {
                portNumber = "9085";
            }
            testRoutine(
                        thisMethod, //String thisMethod,
                        "SymSignatureTokenPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX36Service", //String strServiceName,
                        "UrnX509Token36" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            if (debug) {
                portNumber = tmpPort;
            }
        }

        return;
    }

    //
    // Next section is test in negative ways. The policy of service-client does not match with the service-provider
    //

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * And the service need the x509 to sign and encrypt the SOAPBody
     * The client send the request with EncryptBeforeSigning
     * But provider ask for SignBeforeEncrypting
     *
     */

    @Test
    @AllowedFFDC(value = { "org.apache.ws.security.WSSecurityException", "java.lang.Exception" })
    public void testBadCxfX509KeyIdMigSymEncryptBeforeSigningService() throws Exception {
        String thisMethod = "testCxfX509KeyIdMigSymEncryptBeforeSigningService";
        methodFull = "testBadCxfX509KeyIdMigSymEncryptBeforeSigningService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509KeyIdentifierEncryptBeforeSigningPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX01Service", //String strServiceName,
                           "UrnX509Token01" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * The client send a request without EncryptSignature properties
     * But provider has EncryptSignature properties
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadCxfX509IssuerSerialMigSymNoEncryptSignatureService() throws Exception {
        String thisMethod = "testCxfX509IssuerSerialMigSymNoEncryptSignatureService";
        methodFull = "testBadCxfX509IssuerSerialMigSymNoEncryptSignatureService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadX509IssuerSerialPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX03Service", //String strServiceName,
                           "UrnX509Token03" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * And the service need the x509 to sign and encrypt the SOAPBody
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadCxfX509ProtectTokensMigSymService() throws Exception {
        String thisMethod = "testCxfX509ProtectTokensMigSymService";
        methodFull = "testBadCxfX509ProtectTokensMigSymService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509SymmetricProtectTokensPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX09Service", //String strServiceName,
                           "UrnX509Token09" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * And the service need the x509 to sign and encrypt the SOAPBody
     *
     */

    //@Test
    // @to be developed. Currently the key reference is not enforced
    public void testBadCxfX509ThumbprintMigSymService() throws Exception {
        String thisMethod = "testCxfX509ThumbprintMigSymService";
        methodFull = "testBadCxfX509ThumbprintMigSymService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509ThumbprintPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX04Service", //String strServiceName,
                           "UrnX509Token04" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * And the service need the x509 to sign and encrypt the SOAPBody
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadCxfX509KeyIdDerivedMigSymService() throws Exception {
        String thisMethod = "testCxfX509KeyIdDerivedMigSymService";
        methodFull = "testBadCxfX509KeyIdDerivedMigSymService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509KeyIdentifierDerivedPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX05Service", //String strServiceName,
                           "UrnX509Token05" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service.
     * And the service need the x509 to sign and encrypt the SOAPBody
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    // gkuo: To be discussed with Chunlong
    public void testBadCxfX509ThumbprintDerivedMigSymService() throws Exception {
        String thisMethod = "testCxfX509ThumbprintDerivedMigSymService";
        methodFull = "testBadCxfX509ThumbprintDerivedMigSymService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509ThumbprintDerivedPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX06Service", //String strServiceName,
                           "UrnX509Token06" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * The test case test:
     * <sp:EncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp13:Created/>
     * <sp13:Nonce/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:EncryptedSupportingTokens>
     * But we remove it in the local client.
     * It expects to fail.
     *
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadX509KeyIdentifierUNTService() throws Exception {
        String thisMethod = "testBadX509KeyIdentifierUNTService";
        methodFull = "testBadX509KeyIdentifierUNTService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509KeyIdentifierUNTPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX24Service", //String strServiceName,
                           "UrnX509Token24" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * The test case test:
     * <sp:SignedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp13:Created/>
     * <sp13:Nonce/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:SignedSupportingTokens>
     * and also
     * <sp:RequireSignatureConfirmation/>
     * But we take off: SignedSupportingToken.
     * It expects to fail.
     *
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadX509SignatureConfirmService() throws Exception {
        String thisMethod = "testBadX509SignatureConfirmService";
        methodFull = "testBadX509SignatureConfirmService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSym_SignConfirmPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX25Service", //String strServiceName,
                           "UrnX509Token25" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This tests:
     * <sp:SignedSupportingTokens
     * xmlns:sp="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702">
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssUsernameToken10 />
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:SignedSupportingTokens>
     * But we add
     * <sp:RequireSignatureConfirmation/>
     * in client Policy
     * It expects to fail
     *
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadSymEncSignService() throws Exception {
        String thisMethod = "testBadSymEncSignService";
        methodFull = "testBadSymEncSignService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSymEncSignPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX27Service", //String strServiceName,
                           "UrnX509Token27" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This test provide The policy layout
     * We switch the Strict and Lax between the Service Provider and Client Requester
     * ** See the above testEncSignStrictService as well
     * Strict and Lax does not matter.
     * But we do not know if it's ignored or they just follow Strict or Lax
     * In case, this test case, we need to figure out and maybe document
     *
     **/

    @Test
    public void testBadSymEncSignStrictService() throws Exception {
        String thisMethod = "testBadSymEncSignStrictService";
        methodFull = "testBadSymEncSignStrictService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSymEncSignStrictPolicy", // Testing policy name
                           "positive", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX29Service", //String strServiceName,
                           "UrnX509Token29" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * This tests <sp:Basic192/>
     *
     * The client will send request on <sp:Basic128/>
     * It expects to fail all the time even the UnrestrictedJdkSecurityPolicy is implemented
     *
     **/

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadBasic192Service() throws Exception {
        String thisMethod = "testBadBasic192Service";
        methodFull = "testBadBasic192Service";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSymEncSignBasic192Policy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX31Service", //String strServiceName,
                           "UrnX509Token31" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    // This proves that InclusiveC14N is in troubles.
    // With InclusiveC14N
    //   When we specify Basic128Sha256, this test fail
    //   But when we specify Basic128, the test passed.
    //
    // Checked, the InclusiveC14N is not built in Client. It's
    //    .......
    //         <ds:Reference URI="#UsernameToken-100">
    //            <ds:Transforms>
    //               <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
    //                  <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="soapenv"></ec:InclusiveNamespaces>
    //               </ds:Transform>
    //            </ds:Transforms>
    //            <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"></ds:DigestMethod>
    //            <ds:DigestValue>3Uk8HpK4bUmsCpD6sHyOCiis4XM=</ds:DigestValue>
    //         </ds:Reference>
    //      </ds:SignedInfo>
    //  in tcpmon
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadInclusiveC14NService() throws Exception {
        String thisMethod = "testBadInclusiveC14NService";
        methodFull = "testBadInclusiveC14NService";

        try {
            //portNumber = "9085";
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSymEncSignInclusiveC14NPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX33Service", //String strServiceName,
                           "UrnX509Token33" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //
    //  This tests the
    // <sp:EndorsingSupportingTokens xmlns:sp="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702">
    //    <wsp:Policy>
    //         <sp:X509Token
    //            sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
    //            <wsp:Policy>
    //               <sp:RequireThumbprintReference/>
    //               <sp:WssX509V3Token10/>
    //            </wsp:Policy>
    //         </sp:X509Token>
    //     </wsp:Policy>
    // </sp:EndorsingSupportingTokens>
    // With SymmetricBinding
    // But the ws service client exclude the EndorsingSupportingToken tag.
    // It expects to fail
    //
    @Test
    public void testBadSymmetricEndorsingUNTPolicy() throws Exception {
        String thisMethod = "testBadSymmetricEndorsingUNTPolicy";
        methodFull = "testBadSymmetricEndorsingUNTPolicy";

        boolean debug = false;
        String tmpPort = portNumber;
        try {
            if (debug) {
                portNumber = "9085";
            }
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadSymmetricEndorsingUNTPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX35Service", //String strServiceName,
                           "UrnX509Token35" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            if (debug) {
                portNumber = tmpPort;
            }
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have X509 key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testRoutine(
                               String thisMethod,
                               String x509Policy,
                               String testMode, // Positive, positive-1, negative or negative-1... etc
                               String portNumber,
                               String portNumberSecure,
                               String strServiceName,
                               String strServicePort) throws Exception {
        testSubRoutine(
                       thisMethod,
                       x509Policy,
                       testMode, // Positive, positive-1, negative or negative-1... etc
                       portNumber,
                       portNumberSecure,
                       strServiceName,
                       strServicePort,
                       x509MigSymClientUrl,
                       "");

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have X509 key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testBadRoutine(
                                  String thisMethod,
                                  String x509Policy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort) throws Exception {
        testSubRoutine(
                       thisMethod,
                       x509Policy,
                       testMode, // Positive, positive-1, negative or negative-1... etc
                       portNumber,
                       portNumberSecure,
                       strServiceName,
                       strServicePort,
                       x509MigBadSymClientUrl,
                       "Bad");

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a jax-ws cxf web service.
     * It needs to have X509 key set to sign and encrypt the SOAPBody
     * The request is request in https.
     * Though this test is not enforced it yet.
     *
     */
    protected void testSubRoutine(
                                  String thisMethod,
                                  String x509Policy,
                                  String testMode, // Positive, positive-1, negative or negative-1... etc
                                  String portNumber,
                                  String portNumberSecure,
                                  String strServiceName,
                                  String strServicePort,
                                  String strClientUrl,
                                  String strBadOrGood) throws Exception {
        try {

            WebRequest request = null;
            WebResponse response = null;

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            // Invoke the service client - servlet
            Log.info(thisClass, methodFull, "Invoking: " + x509Policy + ":" + testMode);
            request = new GetMethodWebRequest(strClientUrl);

            request.setParameter("serverName", serverName);
            request.setParameter("thisMethod", thisMethod);
            request.setParameter("x509Policy", x509Policy);
            request.setParameter("testMode", testMode);
            request.setParameter("httpDefaultPort", portNumber);
            request.setParameter("httpSecureDefaultPort", portNumberSecure);
            request.setParameter("serviceName", strServiceName);
            request.setParameter("servicePort", strServicePort);
            request.setParameter("methodFull", methodFull);
            Log.info(thisClass, methodFull, "The request is: '" + request);

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            String respReceived = response.getText();
            String methodFull = thisMethod;
            if (strBadOrGood.length() > 0) {
                methodFull = thisMethod.substring(0, 4) + // "test"
                             strBadOrGood +
                             thisMethod.substring(4);
            }
            if (respReceived != null && respReceived.isEmpty()) {
                respReceived = "pass:false:'received nothing'";
            }
            Log.info(thisClass, methodFull, "The response received is: '" + respReceived + "'");
            assertTrue("Failed to get back the expected text. But :" + respReceived, respReceived.contains("<p>pass:true:"));
            assertTrue("Hmm... Strange! wrong testMethod back. But :" + respReceived, respReceived.contains(">m:" + thisMethod + "<"));
        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "Exception occurred:");
            System.err.println("Exception: " + e);
            throw e;
        }

        return;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            printMethodName("tearDown");
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        //orig from CL:
        //SharedTools.unInstallCallbackHandler(server);
    }

    private static void printMethodName(String strMethod) {
        Log.info(thisClass, strMethod, "*****************************"
                                       + strMethod);
        System.err.println("*****************************" + strMethod);
    }

    public static void copyServerXml(String copyFromFile) throws Exception {

        try {
            String serverFileLoc = (new File(server.getServerConfigurationPath().replace('\\', '/'))).getParent();
            Log.info(thisClass, "copyServerXml", "Copying: " + copyFromFile
                                                 + " to " + serverFileLoc);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(),
                                                   serverFileLoc, "server.xml", copyFromFile);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
}
