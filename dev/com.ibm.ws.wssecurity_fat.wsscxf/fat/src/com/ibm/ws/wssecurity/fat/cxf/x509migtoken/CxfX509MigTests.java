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
import componenttest.topology.impl.LibertyServer;

//12/2020 Setting this test class for LITE bucket
//@Mode(TestMode.FULL)
//Added 11/2020
@RunWith(FATRunner.class)
public class CxfX509MigTests {

    //orig from CL:
    //private static String serverName = "com.ibm.ws.wssecurity_fat.x509mig";
    //private static LibertyServer server = LibertyServerFactory.getLibertyServer(serverName);

    //Added 11/2020
    static final private String serverName = "com.ibm.ws.wssecurity_fat.x509mig";
    @Server(serverName)
    public static LibertyServer server;

    static private final Class<?> thisClass = CxfX509MigTests.class;

    static boolean debugOnHttp = true;

    private static String portNumber = "";
    private static String portNumberSecure = "";
    private static String x509MigClientUrl = "";
    private static String x509MigBadClientUrl = "";
    private String methodFull = "";

    static String hostName = "localhost";

    final static String badUsernameToken = "The security token could not be authenticated or authorized";
    final static String msgExpires = "The message has expired";
    final static String badHttpsToken = "HttpsToken could not be asserted";
    final static String badHttpsClientCert = "Could not send Message.";

    static boolean unlimitCryptoKeyLength = false;

    /**
     * Sets up any configuration required for running the OAuth tests.
     * Currently, it just starts the server, which should start the applications
     * in dropins.
     */
    @BeforeClass
    public static void setUp() throws Exception {

        String thisMethod = "setup";

        //orig from CL:
        //SharedTools.installCallbackHandler(server);

        //Added 11/2020
        ShrinkHelper.defaultDropinApp(server, "x509migclient", "com.ibm.ws.wssecurity.fat.x509migclient", "test.libertyfat.x509mig.contract", "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migbadclient", "com.ibm.ws.wssecurity.fat.x509migbadclient", "test.libertyfat.x509mig.contract",
                                      "test.libertyfat.x509mig.types");
        ShrinkHelper.defaultDropinApp(server, "x509migtoken", "basicplcy.wssecfvt.test");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/com.ibm.ws.wssecurity.example.cbh.jar");
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/wsseccbh-1.0.mf");

        server.startServer();// check CWWKS0008I: The security service is ready.
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
        x509MigClientUrl = "http://localhost:" + portNumber +
                           "/x509migclient/CxfX509MigSvcClient";
        x509MigBadClientUrl = "http://localhost:" + portNumber +
                              "/x509migbadclient/CxfX509MigBadSvcClient";
        // portNumber = "9085";                // for debugging
        Log.info(thisClass, thisMethod, "****portNumber is(2):" + portNumber);
        Log.info(thisClass, thisMethod, "****portNumberSecure is(2):" + portNumberSecure);

        // Check the JDK supports crypto key length > 128
        try {
            unlimitCryptoKeyLength = Cipher.getMaxAllowedKeyLength("RC5") > 128;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;

    }

    /**
     * TestDescription:
     *
     * Test AsymmetricIssuerSerialPolicy
     * <!-- bax02 -->
     * <wsp:Policy wsu:Id="X509AsymmetricIssuerSerialPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding>
     * <wsp:Policy>
     * <sp:InitiatorToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorToken>
     * <sp:RecipientToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientToken>
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
     * </sp:AsymmetricBinding>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-AF9958B1292A8AA07513560308325864">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-1">
     * <wsu:Created>
     * 2012-12-20T19:13:52.553Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:18:52.553Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-AF9958B1292A8AA07513560308326385">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference>
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
     * xxNCDeB5l79gucdHEq9fZnI3Bk5BGIlOUzEqdr5mJx9ecQT7pzkaa9I6GdNE3W3H/nFdBRzfWNL+cMOEAmyYMfJ4BAVabvP5iLaTQgv2RUPugGe98Ay7DoKNqVaQQCRbirwE049NfjBeMWMcnKxoukFyU56qEUuH5p4Q8EBo0L8=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * <xenc:ReferenceList>
     * <xenc:DataReference URI="#ED-3"/>
     * </xenc:ReferenceList>
     * </xenc:EncryptedKey>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-2">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-1">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * pTkcnAvayLDK1wSrpU5AxiajGCM=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id--565795114">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * KP3fvMJS815LX02UldFLmn2/yK8=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * Ku0Euw3EZaAzuj8ae9lr3yieqsGgYEgD/71/+mvvxqv5veHlgfrpMLu+sZj2e6u2RK64Duq6L2BdcCuffqwtciuFcN58FjUNKFIaitXN+IG7NBW64QQHa+cv/jB5pTVGAIUSLl8xisyQbXH2WR8KZbJFAwUGtD9tytj25B0kWN4=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-AF9958B1292A8AA07513560308325832">
     * <wsse:SecurityTokenReference wsu:Id="STR-AF9958B1292A8AA07513560308325843">
     * <ds:X509Data>
     * <ds:X509IssuerSerial>
     * <ds:X509IssuerName>
     * CN=test1,O=IBM,C=US
     * </ds:X509IssuerName>
     * <ds:X509SerialNumber>
     * 1353451342
     * </ds:X509SerialNumber>
     * </ds:X509IssuerSerial>
     * </ds:X509Data>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--565795114">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-3"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-AF9958B1292A8AA07513560308326385"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * OWeQbtUgg4/1FR0Gu+3E7K3YyF1lNo28oy9ZSRt9Tc3/BG8BK1Zz+ABZOpGrgYdp7EsWQUnzPstPq+JRnNOFgz/NIDfhOfl0oiiRq1wxChR5ts6z93lay9Mqwh+Xj/56UbZ1XZAXXlV3KOXbwVklo4XUPtz/1ic3nPwXQAyLqR0=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymIssuerSerialMigService() throws Exception {
        String thisMethod = "testCxfX509AsymIssuerSerialMigService";
        methodFull = "testCxfX509AsymIssuerSerialMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricIssuerSerialSha256Policy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX02Service", //String strServiceName,
                        "UrnX509Token02" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test Asymmetric AsymmetricThumbprintPolicy
     * <!-- bax07 -->
     * <wsp:Policy wsu:Id="X509AsymmetricThumbprintPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding>
     * <wsp:Policy>
     * <sp:InitiatorToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireThumbprintReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorToken>
     * <sp:RecipientToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireThumbprintReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientToken>
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
     * </sp:AsymmetricBinding>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-AF9958B1292A8AA075135603083314413">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-7">
     * <wsu:Created>
     * 2012-12-20T19:13:53.141Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:18:53.141Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-AF9958B1292A8AA075135603083315114">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference>
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1">
     * g0pNyKdzNr5oceeQwE2Ema44cE8=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * fD9anbDmJ/bz0PB9zmo75kwPg8EABWgbEywqx6o+texuf4uoLId7X/ZDnCG/0teLbUy1oE9Ww4TC70jqh9CrJLFTlKeg/g3grXuBDqeRhqLUD1RsQeLfQANq0CG0KQ6OhQPmPenk8DFdNtSi659xZwXjGuhLquwvLxtPPo1n+OI=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * <xenc:ReferenceList>
     * <xenc:DataReference URI="#ED-9"/>
     * </xenc:ReferenceList>
     * </xenc:EncryptedKey>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-8">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-7">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * lNaXXnAQgEYFmnX4UZbSviMB8kU=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id--1889382260">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * 17sl3bbj3SSEj8VNmr9FGSqTo/E=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * bXathXTKNtds4icQHC130sEjEmMVOo6DgY5QbE8olKuAO1K/cKWNwj/i8z/NmOxYPmUTcT0aXZGgAP3gWGOmrBCn8din/RG3vQ81Dv45voKHBXB93aUv4zrobFiumKOxoVib7FFtJ6OiwwunCBcLQTkSQEnBHcaRddGb0yNzf+Q=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-AF9958B1292A8AA075135603083314411">
     * <wsse:SecurityTokenReference wsu:Id="STR-AF9958B1292A8AA075135603083314412">
     * <wsse:KeyIdentifier EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1">
     * MmL6ZVNPZ2yy9i3zDDJ4L0LKihw=
     * </wsse:KeyIdentifier>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--1889382260">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-9"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-AF9958B1292A8AA075135603083315114"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * NntDLiO/D8tK2iDSr6IgLqUkrERTP9eyBWC5/cCd1+UlkvFw1cDpnD8/WWu70M+sIN3wjsamwhdvvQfiXtTesPhASryHCWOd+T/0768N5pm4W2bZwZbTinAJpR4GL07pFS9vDZg4IJbBBZz29aQeoA==
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymThumbprintMigService() throws Exception {
        String thisMethod = "testCxfX509AsymThumbprintMigService";
        methodFull = "testCxfX509AsymThumbprintMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricThumbprintPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX07Service", //String strServiceName,
                        "UrnX509Token07" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test AsymmetricProtectTokensPolicy
     * <!-- bax08 -->
     * <wsp:Policy wsu:Id="X509AsymmetricProtectTokensPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding>
     * <wsp:Policy>
     * <sp:InitiatorToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorToken>
     * <sp:RecipientToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp/>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:ProtectTokens/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:AsymmetricBinding>
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
     *
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-AF9958B1292A8AA075135603083339219">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-13">
     * <wsu:Created>
     * 2012-12-20T19:13:53.389Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:18:53.389Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-AF9958B1292A8AA075135603083340322">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference>
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
     * FBjcYICfyCbRWj+MhkI1Q10YcUO2fpWS/3T6aFIIWYShjdpyirnH9crWYLvNbszrFTOGvSRbFTwApttbGy8X/DWqXu/z0LXEJzOD1bbRf1EzEj+MBE+DVzBWApeE73AAzV2HspVe7OnUYbBlY75SVHcNolcnH8DB8Aai7aBj3qQ=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * <xenc:ReferenceList>
     * <xenc:DataReference URI="#ED-15"/>
     * </xenc:ReferenceList>
     * </xenc:EncryptedKey>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-14">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-13">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * QnAPkjS95mUEQnDhETg5mfaujnk=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id--353601541">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * qYfTBTOJvNWqhUk3O6zJcxMRl1M=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#X509-AF9958B1292A8AA075135603083339219">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * IN3rpdPDlDWNHTLVngUfS84V3Bw=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * ZE+sfMN+0SKFCgjy0mfHhEJVg2pwClbcBPL8EMLQ8yfhzAYGlXx3iNMqc5lyPKFHjDXQWuwr2u0fFigx5AbWmduVTvyNxs15YWwZxg4XbIlFyrRS7c06xmp19c1dL5IwRS7vejeHG4N/cPO2kNNoIttdCMH02p6tiIqbZMhpKq0=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-AF9958B1292A8AA075135603083339220">
     * <wsse:SecurityTokenReference wsu:Id="STR-AF9958B1292A8AA075135603083339221">
     * <wsse:Reference URI="#X509-AF9958B1292A8AA075135603083339219"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--353601541">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-15"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-AF9958B1292A8AA075135603083340322"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * EIZI5l9Q2M+r25CEYz/st5zfMVB9HMlWqpCwa9O6FShNel5E5HSeTu4QUjdlcJl7G3Pfm8SF0otyhCgHI0D/H+BgE0mbWSaKldqzYOg/5d9BVrTkhLKbC6wfMtwQvOTFK8pZvLowJ6P1Fhqs45c6OK2oPv1M1O/8Jr+RUasXHDI=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymProtectTokensMigService() throws Exception {
        String thisMethod = "testCxfX509AsymProtectTokensMigService";
        methodFull = "testCxfX509AsymProtectTokensMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricProtectTokensPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX08Service", //String strServiceName,
                        "UrnX509Token08" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This is a negative test.
     * Test TransportEndorsingPolicy. See the policy in testCxfX509TransportEndrosingMigServiceHttps
     * But this does not request in Https, whcih is required
     * Expect it to throws Exception in the service-client
     *
     */

    @Test
    public void testCxfX509TransportEndrosingMigService() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingMigService";
        methodFull = "testCxfX509TransportEndorsingMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX10Service", //String strServiceName,
                        "UrnX509Token10" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportEndorsingPolicy. This does send in HTTPS as required
     * <!-- bax10.https -->
     * <wsp:Policy wsu:Id="X509TransportEndorsingPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:EndorsingSupportingTokens>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:EndorsingSupportingTokens>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188459828">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-19">
     * <wsu:Created>
     * 2012-12-21T17:44:44.595Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:44.595Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-20">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-19">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * RqL9YpjWjngW8WxtKc03Z6WGCmA=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * BdZ60CHoAkngA+2dxWL0W9WSnFe+3TgOPmIQqGhWl3B7Wr8dEBpLuyXj+7UNoIks6FiFwMlk22+BlQPJBV9noTPX1n8Z8ufmMu4DiyC5mZCZ1/pmzv9fmrMYpyP0ISFaUB2QByhU26oDp9E0AyDZYdKyNiufzH5YLp8CX6DcG04=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188459829">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188459830">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188459828"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types">
     * <invoke>
     * X509TransportEndorsingPolicy:positive:testCxfX509TransportEndorsingMigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     *
     *
     */

    @Test
    public void testCxfX509TransportEndrosingMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingMigService";
        methodFull = "testCxfX509TransportEndorsingMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX10Service", //String strServiceName,
                        "UrnX509Token10" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This a negative test
     * Test policy TransportEndorsingSP11Policy but does not send in HTTPS which is required
     *
     *
     */

    @Test
    public void testCxfX509TransportEndrosingSP11MigService() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingSP11MigService";
        methodFull = "testCxfX509TransportEndorsingSP11MigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingSP11Policy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX11Service", //String strServiceName,
                        "UrnX509Token11" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportEndorsingSP11Policy. This does send in HTTPS as required
     * <!-- bax11.https -->
     * <wsp:Policy wsu:Id="X509TransportEndorsingSP11Policy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:EndorsingSupportingTokens xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:Policy>
     * </sp:EndorsingSupportingTokens>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188495031">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-22">
     * <wsu:Created>
     * 2012-12-21T17:44:44.947Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:44.947Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-23">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-22">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * c95rLpEUg2Rsqv8Ai9lKBcnYakU=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id-666352839">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * ouS/S9PtTZuNu7C8y/txKJNp/+s=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * Iz8SdWrwYXyP0KbIjogHuLXIbNcxniAqQRi7pnX2T9scZKB0RsXxg/4X45nweKhjtBTWU8o1T2yh0sBrzIVmIftaKdInug7amSEJH3V6H/ePY9XhOR1hz0ck+Ns23YeqkNT8+Bsv9VWn4dgFH+xvcWxZGifiTufYIF/HUeT60Vk=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188495032">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188495033">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188495031"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-666352839">
     * <invoke>
     * X509TransportEndorsingSP11Policy:positive:testCxfX509TransportEndorsingSP11MigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509TransportEndrosingSP11MigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingSP11MigService";
        methodFull = "testCxfX509TransportEndorsingSP11MigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingSP11Policy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX11Service", //String strServiceName,
                        "UrnX509Token11" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This is a negative test.
     * See the policy in testCxfX509TransportSignedEndrosingMigServiceHttps()
     * This does not send request in HTTPS. So, it fails in service-client
     *
     */

    @Test
    public void testCxfX509TransportSignedEndrosingMigService() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingMigService";
        methodFull = "testCxfX509TransportSignedEndorsingMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSignedEndorsingPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX12Service", //String strServiceName,
                        "UrnX509Token12" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportSignedEndorsingPolicy. This does send request in HTTPS.
     * <!-- bax12.https -->
     * <wsp:Policy wsu:Id="X509TransportSignedEndorsingPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:SignedEndorsingSupportingTokens>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:SignedEndorsingSupportingTokens>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188534434">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-25">
     * <wsu:Created>
     * 2012-12-21T17:44:45.342Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:45.342Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-26">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-25">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * QhwWRCUah0DOUfl4ddkjgJeCvZo=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * omeTE51G9cMCujHYLcrzxZOT0gEqJEfRM6CBkDMTlD/1LZALFcAxvUwbSBeGfmkuwJzjR5O5xRgShNtD0F1Fl+5Ur0/AZA4ssoYfAwX3GrM3dEPz94QXvepMGMJ6ioBKU21dcoJA127sHGtOtdklMyJfr1d/mSCkBvRFwoMviys=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188534435">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188534436">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188534434"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types">
     * <invoke>
     * X509TransportSignedEndorsingPolicy:positive:testCxfX509TransportSignedEndorsingMigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509TransportSignedEndrosingMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingMigService";
        methodFull = "testCxfX509TransportSignedEndorsingMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSignedEndorsingPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX12Service", //String strServiceName,
                        "UrnX509Token12" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This is a negative test case.
     * See the policy in testCxfX509TransportEndrosingEncryptedMigServiceHttps()
     * The service-client does not send in HTTPS. It ought to receive an Exception
     *
     */

    @Test
    public void testCxfX509TransportEndrosingEncryptedMigService() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingEncryptedMigService";
        methodFull = "testCxfX509TransportEndorsingEncryptedMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingEncryptedPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX13Service", //String strServiceName,
                        "UrnX509Token13" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TrandportEndorsingEncyyptedPolicy. service-client sends request in HTTPS as required.
     * <!-- bax13.https -->
     * <wsp:Policy wsu:Id="X509TransportEndorsingEncryptedPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:EndorsingEncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:EndorsingEncryptedSupportingTokens>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188576237">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-28">
     * <wsu:Created>
     * 2012-12-21T17:44:45.760Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:45.760Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-29">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-28">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * s0ka2aduF2+0c2UbjtpFUsAEXTU=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * m/a95Q2PAnOrXi4GT2xi5tk17fP+Lr/yUseWwrNW+u35mFUTuUFI25HoFTAUyQseQfFFIUBrElosUz9lTt17RQICO8VhgQSAAy/UnJBgxv6mYDrTvHCGDTiXyt16box+I7DA7Gr4CEjFXzq045MD6NVRktG6RTK4pD0RdHw3w8Y=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188576238">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188576239">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188576237"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types">
     * <invoke>
     * X509TransportEndorsingEncryptedPolicy:positive:testCxfX509TransportEndorsingEncryptedMigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509TransportEndrosingEncryptedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingEncryptedMigService";
        methodFull = "testCxfX509TransportEndorsingEncryptedMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportEndorsingEncryptedPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX13Service", //String strServiceName,
                        "UrnX509Token13" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This is a negative test.
     * See policy at testCxfX509TransportSignedEndrosingEncryptedMigServiceHttps()
     * The service-client does not send the request in HTTPS which is required.
     * So, it failed as expecting
     *
     */

    @Test
    public void testCxfX509TransportSignedEndrosingEncryptedMigService() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingEncryptedMigService";
        methodFull = "testCxfX509TransportSignedEndorsingEncryptedMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSignedEndorsingEncryptedPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX14Service", //String strServiceName,
                        "UrnX509Token14" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportSignedEndorsingEncryptedPolicy. Service-client sends the request in HTTPS as required.
     * <!-- bax14.https -->
     * <wsp:Policy wsu:Id="X509TransportSignedEndorsingEncryptedPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:SignedEndorsingEncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:SignedEndorsingEncryptedSupportingTokens>
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
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188628240">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-31">
     * <wsu:Created>
     * 2012-12-21T17:44:46.279Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:46.279Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-32">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-31">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * w+1z1Bi/dXgW5JXWVq66tQtg/M0=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * JVQkzRjjdWxvs+7NnJgpWvgIuWGkuY1kCOIRq6zvUxliKvpoCXPLx9mW8Wk9X2yuAVrpdVZChSXIcLQNWVJmBH8ugCyrYipiSFjWfBIPk5Mt7P0k7MaQ1aza8929Znryt8BtFf3pcLxZ1+Bl56K57LuSA8fKZb/8+cuIDmfDhl8=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188628241">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188628242">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188628240"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types">
     * <invoke>
     * X509TransportSignedEndorsingEncryptedPolicy:positive:testCxfX509TransportSignedEndorsingEncryptedMigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     *
     */

    @Test
    public void testCxfX509TransportSignedEndrosingEncryptedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingEncryptedMigService";
        methodFull = "testCxfX509TransportSignedEndorsingEncryptedMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSignedEndorsingEncryptedPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX14Service", //String strServiceName,
                        "UrnX509Token14" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This is a negative test
     * See the policy intestCxfX509TransportSupportingSignedMigServiceHttps()
     * The service-client does not send the web-service request in https
     * So, it fails as expecting
     *
     */

    @Test
    public void testCxfX509TransportSupportingSignedMigService() throws Exception {
        String thisMethod = "testCxfX509TransportSupportingSignedMigService";
        methodFull = "testCxfX509TransportSupportingSignedMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSupportingSignedPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX15Service", //String strServiceName,
                        "UrnX509Token15" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportSupportingSignedPolicy. The service-client sends request in HTTPS
     * <!-- bax15.https -->
     * <wsp:Policy wsu:Id="X509TransportSupportingSignedPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <!-- wsaws:UsingAddressing xmlns:wsaws="http://www.w3.org/2006/05/addressing/wsdl" / -->
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:EndorsingSupportingTokens>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * <sp:SignedParts>
     * <sp:Body/>
     * <sp:Header Name="To" Namespace="http://www.w3.org/2005/08/addressing"/>
     * </sp:SignedParts>
     * <sp:SignedElements>
     * <sp:XPath>//*[local-name()='ReplyTo']</sp:XPath>
     * </sp:SignedElements>
     * </wsp:Policy>
     * </sp:EndorsingSupportingTokens>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <Action xmlns="http://www.w3.org/2005/08/addressing">
     * http://cxf.apache.org/jaxws/dispatch/FVTVersionBAX/InvokeRequest
     * </Action>
     * <MessageID xmlns="http://www.w3.org/2005/08/addressing">
     * urn:uuid:7f858ac1-5ad7-489c-8261-07fe13648cd1
     * </MessageID>
     * <To xmlns="http://www.w3.org/2005/08/addressing"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-2027878003">
     * https://localhost:8020/x509migtoken/FatBAX15Service
     * </To>
     * <ReplyTo xmlns="http://www.w3.org/2005/08/addressing"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--627649104">
     * <Address>
     * http://www.w3.org/2005/08/addressing/anonymous
     * </Address>
     * </ReplyTo>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsse:BinarySecurityToken EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
     * wsu:Id="X509-8C2C1D9E285B34EB52135611188704443">
     * MIIBzTCCATagAwIBAgIEUKwHTjANBgkqhkiG9w0BAQQFADArMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQ4wDAYDVQQDEwV0ZXN0MTAeFw0xMjExMjAyMjQyMjJaFw0zNDEwMTYyMjQyMjJaMCsxCzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xDjAMBgNVBAMTBXRlc3QxMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCs1Q056xyjEXo9kNEg
     * /iIuwvB3Q3ABE1vBHNi2JOQtSWaLmyFDh5+
     * HWJIeM5CNmY5h6CmTCLsys8fDHLft7xqsia6XbVdNx6mNHg4jFxuvv2DgCsKv6iiVbjQE7TIMDJpz6M4eWGUzERtKTnFWOgCegcw7fgWfv5InuBiymzxDkwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAEneC2XBCMu32aI4
     * /28BHHiOOAIMmkT84mr5XoBheNEW+zzCclN7icUDsXi+VtvT0fP98FIr5z4m3bPgxhom8aspcjiNEYtc9cHYrgaqsyKdZm4aZC/ceL+JnUeMAyMLwA8mCPwrbLasy+bWGqE7yscJC8I/yWkG3w4C1PI+Dk5T
     * </wsse:BinarySecurityToken>
     * <wsu:Timestamp wsu:Id="TS-34">
     * <wsu:Created>
     * 2012-12-21T17:44:46.967Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-21T17:49:46.967Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-35">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-34">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * A/cY0USxYG1DaC9lQ8pDZOQZPJs=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id-1916580962">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * MdnqV06Tq663e9EFCAfm4nRdhoE=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id-2027878003">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * E0U2kzEb43Fe23MSwMYl6Ao8e/I=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#Id--627649104">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * gCXD2bC50FS/ZkgXRETRfVECFkk=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * bPTdXmZGdIMn25wE/ONCY9yUpVOpeDFnMUxvnOgXlqfOQ9xZe8GejBZe3h237IwaVgDx9sethUDUvenemPTOEh7mH1Yno+msvoJGVStxbZacmNu3fn6HKNAzD6AuwGD9p+K1/7RKoQSmzwDcg9lgmq/LbCjkr5jPwUVvqhWBtQk=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-8C2C1D9E285B34EB52135611188704444">
     * <wsse:SecurityTokenReference wsu:Id="STR-8C2C1D9E285B34EB52135611188704445">
     * <wsse:Reference URI="#X509-8C2C1D9E285B34EB52135611188704443"
     * ValueType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-1916580962">
     * <invoke>
     * X509TransportSupportingSignedPolicy:positive:testCxfX509TransportSupportingSignedMigServiceHttps
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509TransportSupportingSignedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSupportingSignedMigService";
        methodFull = "testCxfX509TransportSupportingSignedMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportSupportingSignedPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX15Service", //String strServiceName,
                        "UrnX509Token15" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test TransportKVTPolicy
     * The HTTPS is required. But service-client does not do so. It ought to fail
     *
     */
    // @Test --debug-gkuo
    // Somehow the test is failing. Talk to Chunlong, it's not supported
    public void testCxfX509TransportKVTMigService() throws Exception {
        String thisMethod = "testCxfX509TransportKVTMigService";
        methodFull = "testCxfX509TransportKVTMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportKVTPolicy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX16Service", //String strServiceName,
                        "UrnX509Token16" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * <!-- bax16.https -->
     * <wsp:Policy wsu:Id="X509TransportKVTPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:TransportBinding>
     * <wsp:Policy>
     * <sp:TransportToken>
     * <wsp:Policy>
     * <sp:HttpsToken>
     * <wsp:Policy/>
     * </sp:HttpsToken>
     * </wsp:Policy>
     * </sp:TransportToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax />
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:IncludeTimestamp />
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128 />
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:TransportBinding>
     * <sp:EndorsingSupportingTokens>
     * <wsp:Policy>
     * <sp:KeyValueToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:RsaKeyValue />
     * </wsp:Policy>
     * </sp:KeyValueToken>
     * </wsp:Policy>
     * </sp:EndorsingSupportingTokens>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     *
     * ** Example SOAPMessage the service-client sent
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-37">
     * <wsu:Created>
     * 2012-12-19T21:10:47.619Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-19T21:15:47.619Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-38">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
     * <ds:Reference URI="#TS-37">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * ORvRnIrDmlGzQk1baKOX8L0chQI=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * aS8RnH8BHkFp65kfoOGHrS1nFXJhQKpebUrT15uwwvh/OHeJRX5ip1RplvEMLeOqV/YstAzXQMAebPoDI1zmDPR/606i/djhYWUyspaZ3uZI4qnay+4Za20sygi3Zp1xiAxMzLGCXKSJoZcEgLE7o6TxQsku5WcrIZy5Y8XkG6A=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-D3A9AD7A938A185CF3135595144762147">
     * <ds:KeyValue>
     * <ds:RSAKeyValue>
     * <ds:Modulus>
     * rNUNOescoxF6PZDRIP4iLsLwd0NwARNbwRzYtiTkLUlmi5shQ4efh1iSHjOQjZmOYegpkwi7MrPHwxy37e8arImul21XTcepjR4OIxcbr79g4ArCr+oolW40BO0yDAyac+jOHlhlMxEbSk5xVjoAnoHMO34Fn7+SJ7gYsps8Q5M=
     * </ds:Modulus>
     * <ds:Exponent>
     * AQAB
     * </ds:Exponent>
     * </ds:RSAKeyValue>
     * </ds:KeyValue>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types">
     * <invoke>
     * X509TransportKVTPolicy:positive
     * </invoke>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */
    // @Test
    // Somehow the test is failing. Talk to Chunlong, it's not supported
    public void testCxfX509TransportKVTMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportKVTMigService";
        methodFull = "testCxfX509TransportKVTMigServiceHttps";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509TransportKVTPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        portNumberSecure, //String portNumberSecure
                        "FatBAX16Service", //String strServiceName,
                        "UrnX509Token16" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test AsymmetricSignaturePolicy
     * <!-- bax17 -->
     * <wsp:Policy wsu:Id="X509AsymmetricSignaturePolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding>
     * <wsp:Policy>
     * <sp:InitiatorSignatureToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorSignatureToken>
     * <sp:RecipientSignatureToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientSignatureToken>
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
     * </sp:AsymmetricBinding>
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
     * <wsu:Timestamp wsu:Id="TS-51">
     * <wsu:Created>
     * 2012-12-20T19:14:20.490Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:20.490Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B36135603086049341">
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
     * B02DGzU4KX26ckLKYBUr5a+WLcOml06lc3py/YWgjooWNGJFvcDvhcbzQ71KSDov98mPxN/w8VAgPl17cNIRrmjwnxkAtAFmELnbBn/orD98RBRrpEZdUIw/UdWZzscAl5tnsJa/O92wJMntdWsnvnKhFvRFqGdee1IjCul+qvk=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-53"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-52">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id--1418292366">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * BzLpiCY+vUCY++yAFLvxByS8lDw=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-51">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * PPAhAyO05H1kPPeHvO2QWlepp7U=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * 5vCz+9IBFqo2i/BsUyaL5GT+WSY=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B36135603086049442">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B36135603086049443">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086049341"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id--1418292366">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-53"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B36135603086049341"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * pQ2GjrhQCqp4iSRY6IvLWgAOFjBhEI5dH6k43K0d/icQXCRNpEzan5angI2YVvH2307xQtF+uajtMnlKBuJSNNFy49CtfaHMhu/53tXzqxBpv1k026K+t8UGXofHvbxgN6sR/UVM5/eK4ZV5LnqKOM+pZgBteA9NqJntOns2Uwo=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymmetricSignatureMigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricSignatureMigService";
        methodFull = "testCxfX509AsymmetricSignatureMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricSignaturePolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX17Service", //String strServiceName,
                        "UrnX509Token17" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This also test AsymmetricSignaturePolicy
     * But it sends the web-service twice.
     * The second request is considered as Replay attack. and Will fail
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testCxfX509AsymmetricSignatureReplayMigService() throws Exception {
        // This is a negative test case.
        // It ought to fail at the second request.
        // but the first request is a positive
        String thisMethod = "testCxfX509AsymmetricSignatureReplayMigService";
        methodFull = "testCxfX509AsymmetricSignatureReplayMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricSignaturePolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        // At the replay, it will be negative
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX17Service", //String strServiceName,
                        "UrnX509Token17" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test AsymmtricSignatureSP11Policy
     * <!-- bax18 -->
     * <wsp:Policy wsu:Id="X509AsymmetricSignatureSP11Policy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding xmlns:sp="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy">
     * <wsp:Policy>
     * <sp:InitiatorSignatureToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorSignatureToken>
     * <sp:RecipientSignatureToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientSignatureToken>
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
     * </sp:AsymmetricBinding>
     * <sp:SignedParts>
     * <sp:Body/>
     * </sp:SignedParts>
     * </wsp:All>
     * </wsp:ExactlyOne>
     * </wsp:Policy>
     *
     *
     * **An example SOAPMessage the service-client sends
     * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
     * <soapenv:Header>
     * <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * soapenv:mustUnderstand="1">
     * <wsu:Timestamp wsu:Id="TS-143">
     * <wsu:Created>
     * 2012-12-20T19:14:23.256Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:23.256Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B361356030863258117">
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
     * Dw0S2gXew0krm2EKK2wbKE52QSZcfTf+oeIWUX3zOwi03R8YqbXWNtuZ/iamgSbRFFy1WktYaoRiV9N5mPMRYUwL+I79GJ9fhLYtuUUPL5Dilm7Gu1B5Y5qX6yDUA5o8IkxcJ4KE2OGJU6YOSkfLbYrgO8gVdtRfaJpfIjDZn2c=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-145"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-144">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-605864656">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * VH5a25usDGPheIA/4jH09/TUw/0=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-143">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * xLpeB2YywzEp77bdUCWZB+oCKbk=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * ELAukKcx3Lv4jDJBTuzlJjbgm94=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B361356030863261118">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B361356030863261119">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B361356030863258117"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-605864656">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-145"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B361356030863258117"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * fI9gSOq+NeF0ZZGrlOZvQGW+EumLybAP0PMyCz+nOaSuW4FGwbU1+UKslbRZcQr1s2TqfBXgY/go51O+RhOmBKTiSKZoYNd/3l4e9tCeRaNgYvh5RuKXbER48ZNnY+wJr94Jl4++OQFhwPoaTYKUNEf6CBWFwIZmtNKfFvzEWyc=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymmetricSignatureSP11MigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricSignatureSP11MigService";
        methodFull = "testCxfX509AsymmetricSignatureSP11MigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricSignatureSP11Policy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX18Service", //String strServiceName,
                        "UrnX509Token18" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Test AsymmetricEncryptionPolicy
     * <!-- bax19 -->
     * <wsp:Policy wsu:Id="X509AsymmetricEncryptionPolicy">
     * <wsp:ExactlyOne>
     * <wsp:All>
     * <sp:AsymmetricBinding>
     * <wsp:Policy>
     * <sp:InitiatorEncryptionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:InitiatorEncryptionToken>
     * <sp:RecipientEncryptionToken>
     * <wsp:Policy>
     * <sp:X509Token
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/Never">
     * <wsp:Policy>
     * <sp:WssX509V3Token10 />
     * <sp:RequireIssuerSerialReference />
     * </wsp:Policy>
     * </sp:X509Token>
     * </wsp:Policy>
     * </sp:RecipientEncryptionToken>
     * <sp:Layout>
     * <wsp:Policy>
     * <sp:Lax/>
     * </wsp:Policy>
     * </sp:Layout>
     * <sp:OnlySignEntireHeadersAndBody/>
     * <sp:AlgorithmSuite>
     * <wsp:Policy>
     * <sp:Basic128/>
     * </wsp:Policy>
     * </sp:AlgorithmSuite>
     * </wsp:Policy>
     * </sp:AsymmetricBinding>
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
     * <wsu:Timestamp wsu:Id="TS-149">
     * <wsu:Created>
     * 2012-12-20T19:14:23.375Z
     * </wsu:Created>
     * <wsu:Expires>
     * 2012-12-20T19:19:23.375Z
     * </wsu:Expires>
     * </wsu:Timestamp>
     * <xenc:EncryptedKey xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="EK-EEF851A0D5D1683B361356030863377122">
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
     * Drqm03JPRwENpoWTb8HAiQGYjS54RqidlbFALYCP6cu9DfGKdQHSbBTnKLwB/ROUHNBnz7dpc+o46RuDMiBmRs1nkW7TQ7gUVgooxM91fcX5y/IH2NiZReY8i9kHoPMqthnLr1hB2H8VMd7uk1N0lpQJCYxk7TTAZtohMNHk/Sc=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedKey>
     * <xenc:ReferenceList xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
     * <xenc:DataReference URI="#ED-151"/>
     * </xenc:ReferenceList>
     * <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
     * Id="SIG-150">
     * <ds:SignedInfo>
     * <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="soapenv"/>
     * </ds:CanonicalizationMethod>
     * <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#hmac-sha1"/>
     * <ds:Reference URI="#Id-723306099">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList=""/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * /xjaLa50mICdsGYa/WPq66DR8Gg=
     * </ds:DigestValue>
     * </ds:Reference>
     * <ds:Reference URI="#TS-149">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#"
     * PrefixList="wsse soapenv"/>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
     * <ds:DigestValue>
     * kaXFxs/DHpCD9sUWGO4kamEqLMU=
     * </ds:DigestValue>
     * </ds:Reference>
     * </ds:SignedInfo>
     * <ds:SignatureValue>
     * lFhlgVEANXPx2+Qg8Dnf12G0Us0=
     * </ds:SignatureValue>
     * <ds:KeyInfo Id="KI-EEF851A0D5D1683B361356030863380123">
     * <wsse:SecurityTokenReference xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"
     * wsu:Id="STR-EEF851A0D5D1683B361356030863380124">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B361356030863377122"
     * ValueType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * </ds:Signature>
     * </wsse:Security>
     * </soapenv:Header>
     * <soapenv:Body xmlns="http://x509mig.liberty.test/types"
     * xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
     * wsu:Id="Id-723306099">
     * <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
     * Id="ED-151"
     * Type="http://www.w3.org/2001/04/xmlenc#Content">
     * <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
     * <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
     * <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
     * xmlns:wsse11="http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd"
     * wsse11:TokenType="http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey">
     * <wsse:Reference URI="#EK-EEF851A0D5D1683B361356030863377122"/>
     * </wsse:SecurityTokenReference>
     * </ds:KeyInfo>
     * <xenc:CipherData>
     * <xenc:CipherValue>
     * khjTw2e4ybqlZS5IjMeiwfv5XaS/1vku+IY0I325q+5JsRPDAdJ74aQOvpwX91Ex68Pf9UbIfLHVWuYOeCDHp1KWdT+ce9Tp5kci//AALbzOXSBRCH/8y+iWRJqXLt3PFIDuoOSsfDlCdyPZ1L58ENJnP2eBj4CJFUW3Y+jfxeE=
     * </xenc:CipherValue>
     * </xenc:CipherData>
     * </xenc:EncryptedData>
     * </soapenv:Body>
     * </soapenv:Envelope>
     *
     */

    @Test
    public void testCxfX509AsymmetricEncryptionMigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricEncryptionMigService";
        methodFull = "testCxfX509AsymmetricEncryptionMigService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricEncryptionPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX19Service", //String strServiceName,
                        "UrnX509Token19" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple ws-addressing
     *
     */

    @Test
    // Somehow this test fail even it only requests ws-addressing
    // Fixed by defect 90019, related to community bug CXF-4818
    public void testWsAddressingService() throws Exception {
        String thisMethod = "testWsAddressingService";
        methodFull = "testWsAddressingService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "WsAddressingPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX20Service", //String strServiceName,
                        "UrnX509Token20" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * <sp:EncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:HashPassword/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:EncryptedSupportingTokens>
     *
     * and also
     * <sp:EncryptSignature /> <!--Somehow with EncryptBeforeSigning, we have to add this EncryptSignature -->
     * <sp:EncryptBeforeSigning />
     * It expects to pass
     *
     */

    @Test
    public void testWsComplexService() throws Exception {
        String thisMethod = "testWsComplexService";
        methodFull = "testWsComplexService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "WsSignEncryptPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX21Service", //String strServiceName,
                        "UrnX509Token21" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     *
     */

    @Test
    public void testWsComplexSP11Service() throws Exception {
        String thisMethod = "testWsComplexSP11Service";
        methodFull = "testWsComplexSP11Service";
        String tmpPort = portNumber;
        boolean debug = false;
        if (debug)
            portNumber = "9085";
        try {

            testRoutine(
                        thisMethod, //String thisMethod,
                        "WsSignEncryptSP11Policy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX23Service", //String strServiceName,
                        "UrnX509Token23" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        } finally {
            portNumber = tmpPort;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     * using Basic256
     * It's a negative test unless we put in the UnrestrictedJdkSecurityPolicy
     * Once the UnrestrictedJdkSecurityPolicy is implemented. Then this test case ought to fail
     *
     */

    @Test
    public void testAsymmetricBasic256Service() throws Exception {
        String thisMethod = "testAsymmetricBasic256Service";
        methodFull = "testAsymmetricBasic256Service";

        try {
            // If unlimit crypto key length, this is positive test otherwise, negative
            String negpos = unlimitCryptoKeyLength ? "positive" : "negative";
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricIssuerSerialBasic256Policy", // Testing policy name
                        negpos, // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX22Service", //String strServiceName,
                        "UrnX509Token22" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This tests:
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
     * and
     * <sp:RequireSignatureConfirmation/>
     * It expects to pass
     */

    @Test
    public void testAsymSignatureConfirmService() throws Exception {
        String thisMethod = "testAsymSignatureConfirmService";
        methodFull = "testAsymSignatureConfirmService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "Asym_SignatureConfirmPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX26Service", //String strServiceName,
                        "UrnX509Token26" //String strServicePort
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
    public void testAsymEncSignService() throws Exception {
        String thisMethod = "testAsymEncSignService";
        methodFull = "testAsymEncSignService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "AsymEncSignPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX28Service", //String strServiceName,
                        "UrnX509Token28" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Add <sp:LaxTimestampLast/>
     * and test failed.
     */

    //@Test
    // This prove the LaxTimeStampLast is not supported
    public void testAsymEncSignStrictService() throws Exception {
        String thisMethod = "testAsymEncSignStrictService";
        methodFull = "testAsymEncSignStrictService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "AsymEncSignStrictPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX30Service", //String strServiceName,
                        "UrnX509Token30" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Testing <sp:TripleDes/>
     * The test passed.
     *
     */
    @Test
    public void testTripleDesService() throws Exception {
        String thisMethod = "testTripleDesService";
        methodFull = "testTripleDesService";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "AsymEncSignTripleDesPolicy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX32Service", //String strServiceName,
                        "UrnX509Token32" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * The test has only <sp:Basic128/>
     * without <sp:InclusiveC14N/>
     *
     * The test passed.
     *
     */
    @Test
    public void testBasic128Service() throws Exception {
        String thisMethod = "testBasic128Service";
        methodFull = "testBasic128Service";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "AsymEncSignBasic128Policy", // Testing policy name
                        "positive", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX34Service", //String strServiceName,
                        "UrnX509Token34" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    //
    // Next section is test in negative ways. The policy of service-client does not match with the service-provider
    //

    /**
     * TestDescription:
     *
     * The test Service Client is using <sp:Basic256Sha256/>
     * But the Service Provider is expecting <sp:Basic128Sha256/>
     * This is expect tp fail.
     *
     * When unrestrictedJskSecurityPolicy is implemented
     * Then the error message will be different. Since <sp:Basic256Sha256/> is able to be handled.
     * So, the test will fail.
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadCxfX509AsymIssuerSerialMigService() throws Exception {
        String thisMethod = "testBadCxfX509AsymIssuerSerialMigService";
        methodFull = "testBadCxfX509AsymIssuerSerialMigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricIssuerSerialPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX02Service", //String strServiceName,
                           "UrnX509Token02" //String strServicePort
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
     * Client does not handle <sp:EncryptSignature />
     * But provider ask for <sp:EncryptSignature />
     *
     */

    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadCxfX509AsymThumbprintMigService() throws Exception {
        String thisMethod = "testCxfX509AsymThumbprintMigService";
        methodFull = "testBadCxfX509AsymThumbprintMigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricThumbprintPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX07Service", //String strServiceName,
                           "UrnX509Token07" //String strServicePort
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
    public void testBadCxfX509AsymProtectTokensMigService() throws Exception {
        String thisMethod = "testCxfX509AsymProtectTokensMigService";
        methodFull = "testBadCxfX509AsymProtectTokensMigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricProtectTokensPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX08Service", //String strServiceName,
                           "UrnX509Token08" //String strServicePort
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
    public void testBadCxfX509TransportEndrosingMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingMigService";
        methodFull = "testBadCxfX509TransportEndorsingMigServiceHttps";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportEndorsingPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX10Service", //String strServiceName,
                           "UrnX509Token10" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes X509TransportEndorsingSP11Policy without sending a transport token
     *
     */

    @Test
    public void testBadCxfX509TransportEndrosingSP11MigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingSP11MigService";
        methodFull = "testBadCxfX509TransportEndorsingSP11MigServiceHttps";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportEndorsingSP11Policy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX11Service", //String strServiceName,
                           "UrnX509Token11" //String strServicePort
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

    // @Test
    // gkuo: To be discussed with Chunlong
    public void testBadCxfX509TransportSignedEndrosingMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingMigService";
        methodFull = "testBadCxfX509TransportSignedEndorsingMigServiceHttps";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportSignedEndorsingPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX12Service", //String strServiceName,
                           "UrnX509Token12" //String strServicePort
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
    public void testBadCxfX509TransportEndrosingEncryptedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportEndorsingEncryptedMigService";
        methodFull = "testBadCxfX509TransportEndorsingEncryptedMigServiceHttps";
        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportEndorsingEncryptedPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX13Service", //String strServiceName,
                           "UrnX509Token13" //String strServicePort
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
    // gkuo: To be discussed with Chunlong gkuo
    public void testBadCxfX509TransportSignedEndrosingEncryptedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSignedEndorsingEncryptedMigService";
        methodFull = "testBadCxfX509TransportSignedEndorsingEncryptedMigServiceHttps";
        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportSignedEndorsingEncryptedPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX14Service", //String strServiceName,
                           "UrnX509Token14" //String strServicePort
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
    public void testBadCxfX509TransportSupportingSignedMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportSupportingSignedMigService";
        methodFull = "testBadCxfX509TransportSupportingSignedMigServiceHttps";
        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportSupportingSignedPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX15Service", //String strServiceName,
                           "UrnX509Token15" //String strServicePort
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
    public void testBadCxfX509TransportKVTMigServiceHttps() throws Exception {
        String thisMethod = "testCxfX509TransportKVTMigService";
        methodFull = "testBadCxfX509TransportKVTMigServiceHttps";
        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509TransportKVTPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           portNumberSecure, //String portNumberSecure
                           "FatBAX16Service", //String strServiceName,
                           "UrnX509Token16" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * The client ignore this tag: <sp:OnlySignEntireHeadersAndBody/>
     * but the provider enforce it.
     *
     **/

    //@Test
    public void testBadCxfX509AsymmetricSignatureMigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricSignatureMigService";
        methodFull = "testBadCxfX509AsymmetricSignatureMigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricSignaturePolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX17Service", //String strServiceName,
                           "UrnX509Token17" //String strServicePort
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
    public void testBadCxfX509AsymmetricSignatureSP11MigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricSignatureSP11MigService";
        methodFull = "testBadCxfX509AsymmetricSignatureSP11MigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricSignatureSP11Policy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX18Service", //String strServiceName,
                           "UrnX509Token18" //String strServicePort
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
    public void testBadCxfX509AsymmetricEncryptionMigService() throws Exception {
        String thisMethod = "testCxfX509AsymmetricEncryptionMigService";
        methodFull = "testBadCxfX509AsymmetricEncryptionMigService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "X509AsymmetricEncryptionPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX19Service", //String strServiceName,
                           "UrnX509Token19" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * <sp:EncryptedSupportingTokens>
     * <wsp:Policy>
     * <sp:UsernameToken
     * sp:IncludeToken="http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702/IncludeToken/AlwaysToRecipient">
     * <wsp:Policy>
     * <sp:HashPassword/>
     * <sp:WssUsernameToken11/>
     * </wsp:Policy>
     * </sp:UsernameToken>
     * </wsp:Policy>
     * </sp:EncryptedSupportingTokens>
     *
     * and also
     * <sp:EncryptSignature /> <!--Somehow with EncryptBeforeSigning, we have to add this EncryptSignature -->
     * <sp:EncryptBeforeSigning />
     *
     * Provider asks for EncryptBeforeSigning
     * But Client is sending SignBeforEncrypting
     * It expects to fail
     *
     */

    @Test
    public void testBadWsComplexService() throws Exception {
        String thisMethod = "testBadWsComplexService";
        methodFull = "testBadWsComplexService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "WsSignEncryptPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX21Service", //String strServiceName,
                           "UrnX509Token21" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * This test invokes a complex policy
     * It encrypt the username token, sign the bod and encrypt it.
     * using Basic256
     * It's a negative test unless we put in the UnrestrictedJdkSecurityPolicy
     * Once the UnrestrictedJdkSecurityPolicy is implemented. Then this test case ought to fail
     *
     */
    //@Test
    // This was an negative test already (Basic256)
    public void testBadX509AsymmetricIssuerSerialBasic256EncryptBeforeSigningPolicy() throws Exception {
        String thisMethod = "testBadX509AsymmetricIssuerSerialBasic256EncryptBeforeSigningPolicy";
        methodFull = "testBadX509AsymmetricIssuerSerialBasic256EncryptBeforeSigningPolicy";

        try {
            testRoutine(
                        thisMethod, //String thisMethod,
                        "X509AsymmetricIssuerSerialBasic256Policy", // Testing policy name
                        "negative", // Positive, positive-1, negative or negative-1... etc
                        portNumber, //String portNumber,
                        "", //String portNumberSecure
                        "FatBAX22Service", //String strServiceName,
                        "UrnX509Token22" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
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
                       x509MigClientUrl);

        return;
    }

    /**
     * This tests:
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
     * and
     * <sp:RequireSignatureConfirmation/>
     * But we take off EncryptedSupportingToken
     * It expects to fail
     *
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadAsymSignatureConfirmService() throws Exception {
        String thisMethod = "testBadAsymSignatureConfirmService";
        methodFull = "testBadAsymSignatureConfirmService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadAsym_SignatureConfirmPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX26Service", //String strServiceName,
                           "UrnX509Token26" //String strServicePort
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
     **/
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadAsymEndSignService() throws Exception {
        String thisMethod = "testBadAsymEncSignService";
        methodFull = "testBadAsymEncSignService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadAsymEnsSignPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX28Service", //String strServiceName,
                           "UrnX509Token28" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Add <sp:LaxTimestampLast/>
     * and test failed.
     *
     */
    // @Test
    // This prove that LaxTimestampLast is not supported
    public void testBadAsymEndSignStrictService() throws Exception {
        String thisMethod = "testBadAsymEncSignStrictService";
        methodFull = "testBadAsymEncSignStrictService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadAsymEnsSignStrictPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX30Service", //String strServiceName,
                           "UrnX509Token30" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * Client s sending <sp:Basic128/>
     * But the Service Provider is expecting <sp:TripleDes/>
     * The test is expected to fail.
     *
     */
    @Test
    @AllowedFFDC("org.apache.ws.security.WSSecurityException")
    public void testBadTripleDesService() throws Exception {
        String thisMethod = "testBadTripleDesService";
        methodFull = "testBadTripleDesService";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadAsymEncSignTripleDesPolicy", // Testing policy name
                           "negative", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX32Service", //String strServiceName,
                           "UrnX509Token32" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
        }

        return;
    }

    /**
     * TestDescription:
     *
     * The request client only <sp:Basic128/> and <sp:InclusiveC14N/>
     * But the service provider only specifys <sp:Basic128/>
     * This test ought to pass as long as the ServiceProvider recongize the
     * <sp:InclusiveC14N/>
     * But we double check with Tcpmon, we found that even we specify
     * <sp:InclusiveC14N/>
     * But the client is still sending:
     * <ds:Reference URI="#TS-99">
     * <ds:Transforms>
     * <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="wsse soapenv"></ec:InclusiveNamespaces>
     * </ds:Transform>
     * </ds:Transforms>
     * <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"></ds:DigestMethod>
     * <ds:DigestValue>xxFvWVhAUvUkHHBbN4PkI34TTCU=</ds:DigestValue>
     * </ds:Reference>
     *
     * The tcpmon shows that Client is sending
     * <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="wsse soapenv"></ec:InclusiveNamespaces>
     * It indicated the <sp:InclusiveC14N/> is ignored by CXF
     *
     * The test passes, since <sp:InclusiveC14N/> is ignored.
     *
     */
    @Test
    public void testBadBasic128Service() throws Exception {
        String thisMethod = "testBadBasic128Service";
        methodFull = "testBadBasic128Service";

        try {
            testBadRoutine(
                           thisMethod, //String thisMethod,
                           "BadAsymEncSignInclusiveC14NPolicy", // Testing policy name
                           "positive", // Positive, positive-1, negative or negative-1... etc
                           portNumber, //String portNumber,
                           "", //String portNumberSecure
                           "FatBAX34Service", //String strServiceName,
                           "UrnX509Token34" //String strServicePort
            );
        } catch (Exception e) {
            throw e;
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
                       x509MigBadClientUrl);

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
                                  String strClientUrl) throws Exception {
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

            // Invoke the client
            response = wc.getResponse(request);

            // Read the response page from client jsp
            String respReceived = response.getText();
            if (respReceived != null && respReceived.isEmpty()) {
                respReceived = "pass:false:'received nothing'";
            }

            Log.info(thisClass, methodFull, "'" + respReceived + "'");
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
}
