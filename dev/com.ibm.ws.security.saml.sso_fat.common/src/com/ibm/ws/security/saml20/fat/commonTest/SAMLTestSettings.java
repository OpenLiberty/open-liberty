/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.TestSettings;

public class SAMLTestSettings extends TestSettings {

    public static SAMLCommonTestTools cttools = new SAMLCommonTestTools();
    public static SAMLMessageTools msgUtils = new SAMLMessageTools();

    public class ReplaceVars {
        private String oldValue;
        private String newValue;
        private String location = SAMLConstants.LOCATION_ALL;

        public ReplaceVars(String oldVal, String newVal, String newLoc) {
            oldValue = oldVal;
            newValue = newVal;
            location = newLoc;
        }

        public ReplaceVars() {
        }

        public String getOld() {
            return oldValue;
        }

        public void setOld(String oldVal) {
            oldValue = oldVal;
        }

        public String getNew() {
            return newValue;
        }

        public void setNew(String newVal) {
            newValue = newVal;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String newLoc) {
            location = newLoc;
        }
    }

    public class UpdateTimeVars {
        private String attribute;
        private Boolean addTime;
        private int[] time;
        private Boolean useCurrentTime;

        public UpdateTimeVars(String attribute, Boolean addTime, int[] time, String location, Boolean useCurrentTime) {
            this.attribute = attribute;
            this.addTime = addTime;
            this.time = time.clone();
            this.useCurrentTime = useCurrentTime;
        }

        public UpdateTimeVars() {
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String oldVal) {
            attribute = oldVal;
        }

        public Boolean getAddTime() {
            return addTime;
        }

        public void setAddTime(Boolean addTime) {
            this.addTime = addTime;
        }

        public int[] getTime() {
            return time;
        }

        public void setTime(int[] time) {
            this.time = time.clone();
        }

        public Boolean getUseCurrentTIme() {
            return useCurrentTime;
        }

        public void setUseCurrentTime(Boolean useCurrentTIme) {
            useCurrentTime = useCurrentTIme;
        }

    }

    //	public class TimeValues {
    //		private int day = 0 ;
    //		private int hour = 0 ;
    //		private int min = 0 ;
    //		private int second = 0 ;
    //
    //	}

    public class SAMLTokenValidationData {
        private String nameId;
        private final String issuer;
        private String inResponseTo;
        private String messageID;
        private String encryptionKeyUser;
        private String recipient;
        private String encryptAlg;

        public SAMLTokenValidationData(String inNameId, String inIssuer, String inInResponseTo, String inMessageID, String inEncryptionKeyUser, String inRecipient,
                                       String inEncryptAlg) {
            nameId = inNameId;
            issuer = inIssuer;
            inResponseTo = inInResponseTo;
            messageID = inMessageID;
            encryptionKeyUser = inEncryptionKeyUser;
            recipient = inRecipient;
            encryptAlg = inEncryptAlg;
        }

        // default case, initialize to values in test settings
        public SAMLTokenValidationData() {
            nameId = idpUserName;
            issuer = idpIssuer;
            //            this.inResponseTo = inResponseTo;
            messageID = null;
            encryptionKeyUser = null;
            recipient = null;
            encryptAlg = SAMLConstants.AES128;
        }

        public SAMLTokenValidationData copySamlTokeValidationData(String inNameId, String inIssuer, String inInResponseTo, String inMessageID, String inEncryptionKeyUser,
                                                                  String inRecipient, String inEncryptAlg) {
            return new SAMLTokenValidationData(inNameId, inIssuer, inInResponseTo, inMessageID, inEncryptionKeyUser, inRecipient, inEncryptAlg);
        }

        /* individual setters/getters */
        public String getNameId() {
            return nameId;
        }

        public void setNameId(String inNameId) {
            nameId = inNameId;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String inIssuer) {
            nameId = inIssuer;
        }

        public String getInResponseTo() {
            return inResponseTo;
        }

        public void setInResponseTo(String inInResponseTo) {
            inResponseTo = inInResponseTo;
        }

        public String getMessageID() {
            return messageID;
        }

        public void setMessageID(String inMessageID) {
            messageID = inMessageID;
        }

        public String getEncryptionKeyUser() {
            return encryptionKeyUser;
        }

        public void setEncryptionKeyUser(String inEncryptionKeyUser) {
            encryptionKeyUser = inEncryptionKeyUser;
        }

        public String getRecipient() {
            return recipient;
        }

        public void setRecipient(String inRecipient) {
            recipient = inRecipient;
        }

        public String getEncryptAlg() {
            return encryptAlg;
        }

        public void setEncryptAlg(String inEncryptAlg) {
            encryptAlg = inEncryptAlg;
        }

        public void printSAMLTokenValidationData() {
            String thisMethod = "printSAMLTokenValidationData";

            String indent = "  ";
            Log.info(thisClass, thisMethod, "SAML Token Validation Data: ");
            Log.info(thisClass, thisMethod, indent + "nameID: " + nameId);
            Log.info(thisClass, thisMethod, indent + "issuer: " + issuer);
            Log.info(thisClass, thisMethod, indent + "inResponseTo: " + inResponseTo);
            Log.info(thisClass, thisMethod, indent + "encryptionKeyUser: " + encryptionKeyUser);
            Log.info(thisClass, thisMethod, indent + "recipient: " + recipient);
            Log.info(thisClass, thisMethod, indent + "encryptAlg: " + encryptAlg);
        }

    }

    public class CXFSettings {
        private String testMethod;
        private String clientType;
        private String portNumber;
        private String securePort;
        private final String id;
        private String pw;
        private String serviceName;
        private String servicePort;
        private String sendMsg;
        private String replayTest;
        private String managedClient;
        private String clientWSDLFile;
        private String titleToCheck = "";
        private String bodyPartToCheck = "";
        private String testMode = "";

        public CXFSettings(String inTestMethod, String inClientType, String inPortNumber, String inSecurePort, String inId, String inPw,
                           String inServiceName, String inServicePort, String inSendMsg, String inReplayTest, String inManagedClient, String inClientWSDLFile) {
            testMethod = inTestMethod;
            clientType = inClientType;
            portNumber = inPortNumber;
            securePort = inSecurePort;
            id = inId;
            pw = inPw;
            serviceName = inServiceName;
            servicePort = inServicePort;
            sendMsg = inSendMsg;
            replayTest = inReplayTest;
            managedClient = inManagedClient;
            clientWSDLFile = inClientWSDLFile;
        }

        // default case, initialize to values in test settings
        public CXFSettings() {
            testMethod = "notSet";
            portNumber = "notSet";
            securePort = "notSet";
            id = "notSet";
            pw = "notSet";
            serviceName = "notSet";
            servicePort = "notSet";
            sendMsg = "notSet";
            replayTest = "notSet";
            managedClient = "notSet";
            clientWSDLFile = "";
        }

        public CXFSettings copycxfSettings(String inTestMethod, String inClientType, String inPortNumber, String inSecurePort, String inId, String inPw, String inServiceName,
                                           String inServicePort, String inSendMsg, String inReplayTest, String inManagedClient, String inClientWSDLFile) {
            return new CXFSettings(inTestMethod, inClientType, inPortNumber, inSecurePort, inId, inPw, inServiceName, inServicePort, inSendMsg, inReplayTest, inManagedClient, inClientWSDLFile);
        }

        public void setTitleToCheck(String title) {
            titleToCheck = title;
        }

        public String getTitleToCheck() {
            return titleToCheck;
        }

        public void setBodyPartToCheck(String part) {
            bodyPartToCheck = part;
        }

        public String getBodyPartToCheck() {
            return bodyPartToCheck;
        }

        public String getTestMode() {
            return testMode;
        }

        public void setTestMode(String inTestMode) {
            testMode = inTestMode;
        }

        public String getTestMethod() {
            return testMethod;
        }

        public void setTestMethod(String inTestMethod) {
            testMethod = inTestMethod;
        }

        public String getClientType() {
            return clientType;
        }

        public void setClientType(String inClientType) {
            clientType = inClientType;
        }

        public String getPortNumber() {
            return portNumber;
        }

        public void setPortNumber(String inPortNumber) {
            portNumber = inPortNumber;
        }

        public String getSecurePort() {
            return securePort;
        }

        public void setSecurePort(String inSecurePort) {
            securePort = inSecurePort;
        }

        public String getId() {
            return id;
        }

        public void setId(String inId) {
            testMethod = inId;
        }

        public String getPw() {
            return pw;
        }

        public void setPw(String inPw) {
            pw = inPw;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String inServiceName) {
            serviceName = inServiceName;
        }

        public String getServicePort() {
            return servicePort;
        }

        public void setServicePort(String inServicePort) {
            servicePort = inServicePort;
        }

        public String getSendMsg() {
            return sendMsg;
        }

        public void setSendMsg(String inSendMsg) {
            sendMsg = inSendMsg;
        }

        public String getReplayTest() {
            return replayTest;
        }

        public void setReplayTest(String inReplayTest) {
            replayTest = inReplayTest;
        }

        public String getManagedClient() {
            return managedClient;
        }

        public void setManagedClient(String inManagedClient) {
            managedClient = inManagedClient;
        }

        public String getClientWSDLFile() {
            return clientWSDLFile;
        }

        public void setClientWSDLFile(String inClientWSDLFile) {
            clientWSDLFile = inClientWSDLFile;
        }

        public void printCXFSettings() {
            String thisMethod = "printCXFSettings";

            String indent = "  ";
            Log.info(thisClass, thisMethod, "CXF Test Settings: ");
            Log.info(thisClass, thisMethod, indent + "testMethod: " + testMethod);
            Log.info(thisClass, thisMethod, indent + "clientType: " + clientType);
            Log.info(thisClass, thisMethod, indent + "portNumber: " + portNumber);
            Log.info(thisClass, thisMethod, indent + "securePort: " + securePort);
            Log.info(thisClass, thisMethod, indent + "id: " + id);
            Log.info(thisClass, thisMethod, indent + "pw: " + pw);
            Log.info(thisClass, thisMethod, indent + "serviceName: " + serviceName);
            Log.info(thisClass, thisMethod, indent + "servicePort: " + servicePort);
            Log.info(thisClass, thisMethod, indent + "sendMsg: " + sendMsg);
            Log.info(thisClass, thisMethod, indent + "replayTest: " + replayTest);
            Log.info(thisClass, thisMethod, indent + "managedClient: " + managedClient);
            Log.info(thisClass, thisMethod, indent + "clientWSDLFile: " + clientWSDLFile);
            Log.info(thisClass, thisMethod, indent + "titleToCheck: " + titleToCheck);
        }

    }

    public class RSSettings {
        private String headerName;
        private String headerFormat;
        private String samlTokenFormat;

        public RSSettings(String inHeaderName, String inHeaderFormat, String inSamlTokenFormat) {
            headerName = inHeaderName;
            headerFormat = inHeaderFormat;
            samlTokenFormat = inSamlTokenFormat;
        }

        // default case, initialize to values in test settings
        public RSSettings() {
            headerName = "saml_token";
            headerFormat = cttools
                            .chooseRandomEntry(new String[] { SAMLConstants.SAML_HEADER_1, SAMLConstants.SAML_HEADER_2, SAMLConstants.SAML_HEADER_3, SAMLConstants.SAML_HEADER_4 });
            //			this.headerFormat = cttools.chooseRandomEntry(new String[] {SAMLConstants.SAML_HEADER_2}) ;
            // randomly choose if SAML should be  encoded, compressed and encoded, or left as a string (when it is passed on the invoke of the app)
            // duplicate 3 items in the list so we get a better "random" sampling...
            //			this.samlTokenFormat = cttools.chooseRandomEntry(new String[] {SAMLConstants.ASSERTION_TEXT_ONLY, SAMLConstants.ASSERTION_ENCODED, SAMLConstants.ASSERTION_COMPRESSED_ENCODED, SAMLConstants.TOKEN_TEXT_ONLY, SAMLConstants.ASSERTION_TEXT_ONLY, SAMLConstants.ASSERTION_ENCODED, SAMLConstants.ASSERTION_COMPRESSED_ENCODED, SAMLConstants.TOKEN_TEXT_ONLY});
            //            this.samlTokenFormat = cttools.chooseRandomEntry(new String[] { SAMLConstants.ASSERTION_TEXT_ONLY, SAMLConstants.ASSERTION_ENCODED, SAMLConstants.ASSERTION_COMPRESSED_ENCODED, SAMLConstants.ASSERTION_TEXT_ONLY, SAMLConstants.ASSERTION_ENCODED, SAMLConstants.ASSERTION_COMPRESSED_ENCODED });
            samlTokenFormat = cttools.chooseRandomEntry(SAMLConstants.SAML_TOKEN_FORMATS);
            Log.info(thisClass, "RSSettings", "Choose RS Saml Header Format: " + headerFormat);
            Log.info(thisClass, "RSSettings", "Choose RS Saml Assertion Format: " + samlTokenFormat);
        }

        public RSSettings copyRSSettings(RSSettings inSettings) {
            return new RSSettings(inSettings.getHeaderName(), inSettings.getHeaderFormat(), inSettings.getSamlTokenFormat());
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String inHeaderName) {
            headerName = inHeaderName;
        }

        public String getHeaderFormat() {
            return headerFormat;
        }

        public void setHeaderFormat(String inHeaderFormat) {
            headerFormat = inHeaderFormat;
        }

        public String getSamlTokenFormat() {
            return samlTokenFormat;
        }

        public void setSamlTokenFormat(String inSamlTokenFormat) {
            samlTokenFormat = inSamlTokenFormat;
        }

        public void printRSSettings() {
            String thisMethod = "printRSSettings";

            String indent = "  ";
            Log.info(thisClass, thisMethod, "RS Test Settings: ");
            Log.info(thisClass, thisMethod, indent + "headerName: " + headerName);
            Log.info(thisClass, thisMethod, indent + "headerFormat: " + headerFormat);
            Log.info(thisClass, thisMethod, indent + "samlTokenFormat: " + samlTokenFormat);
        }
    }

    static private final Class<?> thisClass = SAMLTestSettings.class;
    private final TestHelpers testHelpers = new TestHelpers();

    private String[] chosen_IDP_server = null;

    private int serverIndex;
    private String idpRoot;
    private String idpEndPoint;
    private String idpChallenge;
    private String spConsumer;
    private String spTargetApp;
    private String spDefaultApp;
    private String spAlternateApp;
    private String spMetaDataEdpt;
    private String idpIssuer;
    private String idpUserName;
    private String idpUserPwd;
    private String spRegUserName;
    private String spRegUserPwd;
    private Boolean useIdAssertion;
    private String relayState;
    private String accessTokenType;
    private String spCookieName;
    private Boolean includeTokenInSubject;
    private String removeTagInResponse = null;
    private String removeTagInResponse2 = null;
    private ArrayList<ReplaceVars> samlTokenReplaceVars = null;
    private ArrayList<UpdateTimeVars> samlTokenUpdateTimeVars = null;
    private SAMLTokenValidationData samlTokenValidationData = null;
    private int tokenReuseSleep = 0;
    private int sleepBeforeTokenUse = 0;
    private String spLogoutURL;
    private String idpLogoutURL;
    private Boolean localLogoutOnly;
    private String removeAttribAll;
    private Boolean isEncryptedAssertion = false;
    private CXFSettings cxfSettings = null;
    private RSSettings rsSettings = null;
    private String idpHostName = null;
    private String idpHostPort = null;
    private String idpHostSecurePort = null;

    public SAMLTestSettings() {
    }

    public SAMLTestSettings(
                            int inServerIndex,
                            String inIdpRoot,
                            String inIdpEndPoint,
                            String inIdpChallenge,
                            String inSpConsumer,
                            String inSpTargetApp,
                            String inSpDefaultApp,
                            String inSpAlternateApp,
                            String inSpMetaDataEdpt,
                            String inIdpIssuer,
                            String inIdpUserName,
                            String inIdpUserPwd,
                            String inSPRegUserName,
                            String inSPRegUserPwd,
                            Boolean inUseIdAssertion,
                            String inRelayState,
                            String inAccessTokenType,
                            String inSpCookieName,
                            Boolean inIncludeTokenInSubject,
                            ArrayList<ReplaceVars> inSamlTokenReplaceVars,
                            ArrayList<UpdateTimeVars> inSamlTokenUpdateTimeVars,
                            SAMLTokenValidationData inSamlTokenValidationData,
                            int inTokenReuseSleep,
                            int inSleepBeforeTokenUse,
                            String inSPLogoutURL,
                            String inIDPLogoutURL,
                            Boolean inLocalLogoutOnly,
                            Boolean inIsEncryptedAssertion,
                            CXFSettings inCXFSettings,
                            RSSettings inRSSettings,
                            String inIdpHostName,
                            String inIdpHostPort,
                            String inIdpHostSecurePort) {

        serverIndex = inServerIndex;
        idpRoot = inIdpRoot;
        idpEndPoint = inIdpEndPoint;
        idpChallenge = inIdpChallenge;
        spConsumer = inSpConsumer;
        spTargetApp = inSpTargetApp;
        spDefaultApp = inSpDefaultApp;
        spAlternateApp = inSpAlternateApp;
        spMetaDataEdpt = inSpMetaDataEdpt;
        idpIssuer = inIdpIssuer;
        idpUserName = inIdpUserName;
        idpUserPwd = inIdpUserPwd;
        spRegUserName = inSPRegUserName;
        spRegUserPwd = inSPRegUserPwd;
        useIdAssertion = inUseIdAssertion;
        relayState = inRelayState;
        accessTokenType = inAccessTokenType;
        spCookieName = inSpCookieName;
        includeTokenInSubject = inIncludeTokenInSubject;
        // need a copy routine for the list
        samlTokenReplaceVars = copySamlReplaceVars(inSamlTokenReplaceVars);
        samlTokenUpdateTimeVars = copySamlUpdateTimeVars(inSamlTokenUpdateTimeVars);
        samlTokenValidationData = copySamlTokeValidationData(inSamlTokenValidationData);
        tokenReuseSleep = inTokenReuseSleep;
        sleepBeforeTokenUse = inSleepBeforeTokenUse;
        spLogoutURL = inSPLogoutURL;
        idpLogoutURL = inIDPLogoutURL;
        localLogoutOnly = inLocalLogoutOnly;
        isEncryptedAssertion = inIsEncryptedAssertion;
        cxfSettings = copyCXFSettings(inCXFSettings);
        rsSettings = copyRSSettings(inRSSettings);
        idpHostName = inIdpHostName;
        idpHostPort = inIdpHostPort;
        idpHostSecurePort = inIdpHostSecurePort;

    }

    @Override
    public SAMLTestSettings copyTestSettings() {
        return new SAMLTestSettings(serverIndex, idpRoot, idpEndPoint, idpChallenge, spConsumer, spTargetApp, spDefaultApp, spAlternateApp, spMetaDataEdpt, idpIssuer, idpUserName, idpUserPwd, spRegUserName, spRegUserPwd, useIdAssertion, relayState, accessTokenType, spCookieName, includeTokenInSubject, samlTokenReplaceVars, samlTokenUpdateTimeVars, samlTokenValidationData, tokenReuseSleep, sleepBeforeTokenUse, spLogoutURL, idpLogoutURL, localLogoutOnly, isEncryptedAssertion, cxfSettings, rsSettings, idpHostName, idpHostPort, idpHostSecurePort);
    }

    @Override
    public void printTestSettings() {
        String thisMethod = "printTestSettings";

        Log.info(thisClass, thisMethod, "Test Settings: ");
        Log.info(thisClass, thisMethod, "serverIndex: " + serverIndex);
        Log.info(thisClass, thisMethod, "idpRoot: " + idpRoot);
        Log.info(thisClass, thisMethod, "idpEndPoint: " + idpEndPoint);
        Log.info(thisClass, thisMethod, "idpChallenge: " + idpChallenge);
        Log.info(thisClass, thisMethod, "spConsumer: " + spConsumer);
        Log.info(thisClass, thisMethod, "spTargetApp: " + spTargetApp);
        Log.info(thisClass, thisMethod, "spDefaultApp: " + spDefaultApp);
        Log.info(thisClass, thisMethod, "spAlternateApp: " + spAlternateApp);
        Log.info(thisClass, thisMethod, "spMetaDataEdpt: " + spMetaDataEdpt);
        Log.info(thisClass, thisMethod, "idpIssuer: " + idpIssuer);
        Log.info(thisClass, thisMethod, "idpUserName: " + idpUserName);
        Log.info(thisClass, thisMethod, "idpUserPwd: " + idpUserPwd);
        Log.info(thisClass, thisMethod, "spRegUserName: " + spRegUserName);
        Log.info(thisClass, thisMethod, "spRegUserPwd: " + spRegUserPwd);
        Log.info(thisClass, thisMethod, "useIdAssertion: " + useIdAssertion);
        Log.info(thisClass, thisMethod, "relayState: " + relayState);
        Log.info(thisClass, thisMethod, "accessTokenType: " + accessTokenType);
        Log.info(thisClass, thisMethod, "spCookieName" + spCookieName);
        Log.info(thisClass, thisMethod, "includeTokenInSubject: " + includeTokenInSubject);
        Log.info(thisClass, thisMethod, "removeTagInResponse: " + removeTagInResponse);
        Log.info(thisClass, thisMethod, "removeTagInResponse2: " + removeTagInResponse2);
        Log.info(thisClass, thisMethod, "samlTokenReplaceVars: " + printSamlReplaceVars());
        Log.info(thisClass, thisMethod, "samlTokenUpdateTimeVars: " + printSamlUpdateTimeVars());
        printSAMLTokenValidationData();
        Log.info(thisClass, thisMethod, "tokenReuseSleep: " + tokenReuseSleep);
        Log.info(thisClass, thisMethod, "sleepBeforeTokenUse: " + sleepBeforeTokenUse);
        Log.info(thisClass, thisMethod, "spLogoutURL: " + spLogoutURL);
        Log.info(thisClass, thisMethod, "idpLogoutURL: " + idpLogoutURL);
        Log.info(thisClass, thisMethod, "localLogoutOnly: " + localLogoutOnly);
        Log.info(thisClass, thisMethod, "isEncryptedAssertion: " + isEncryptedAssertion);
        Log.info(thisClass, thisMethod, "idpHostName: " + idpHostName);
        Log.info(thisClass, thisMethod, "idpHostPort: " + idpHostPort);
        Log.info(thisClass, thisMethod, "idpHostSecurePort: " + idpHostSecurePort);
        printCXFSettings();
        printRSSettings();

    }

    public void setDefaultTestSettings(String testFlowType, String serverType, String httpStart, String httpsStart) throws Exception {

        if (testFlowType.equals(SAMLConstants.SAML_ONLY_SETUP)) {
            setDefaultSAMLServerTestSettings(httpStart, httpsStart);
            //		eSettings.setOAUTHOPserverType();
            //	helpers.setSAMLServer(aTestServer);
        } else {
            if (testFlowType.equals(SAMLConstants.SAML_OIDC_SAME_MACHINE_SETUP)) {
                setDefaultSAMLServerTestSettings(httpStart, httpsStart);
                setDefaultOIDCServerTestSettings(httpStart, httpsStart);
                //			eSettings.setOIDCOPserverType();
                //helpers.setSAMLOIDCServer(aTestServer);
            } else {
                if (testFlowType.equals(SAMLConstants.SAML_OIDC_DIFF_MACHINES_SETUP)) {
                    if (serverType.equals(SAMLConstants.SAML_SERVER_TYPE)) {
                        setDefaultSAMLServerTestSettings(httpStart, httpsStart);
                        //	helpers.setSAMLServer(aTestServer);
                    } else {
                        setDefaultOIDCServerTestSettings(httpStart, httpsStart);
                        //	helpers.setSAMLOIDCServer(aTestServer);
                    }

                }
            }
        }
    }

    /**
     *
     * @param httpStart
     *                       prefix Url string (ie: http://localhost:DefaultPort)
     * @param httpsStart
     *                       prefix Url string (ie: https://localhost:DefaultSSLPort)
     * @return
     *         returns a TestSettings object with the default values set
     */
    public void setDefaultSAMLServerTestSettings(String httpStart, String httpsStart) throws Exception {

        // do we want generic test settings in this method, or just those specific to the SAML server
        // ie: move references to the external SAML server out of here
        //chosen_TFIM_server = selectTFIMIDPServer() ;
        //		String serverName = getSelectedIDPServerName() ;
        //		String serverPort = getSelectedIDPServerPort() ;
        idpEndPoint = getDefaultDetailIdpEndPoint();
        idpRoot = "localhost";
        //		idpEndPoint = "http://" + serverName + ":" + serverPort + SAMLConstants.FEDERATION_JSP ;
        //idpChallenge =
        setSpecificIDPChallenge(1);
        spConsumer = httpsStart + "/ibm/saml20/defaultSP";
        spTargetApp = httpsStart + SAMLConstants.APP1;
        spDefaultApp = httpsStart + SAMLConstants.APP1;
        spAlternateApp = httpsStart + SAMLConstants.APP2;
        spMetaDataEdpt = httpsStart + "/ibm/saml20/defaultSP/samlmetadata";
        spLogoutURL = httpsStart + "/samlclient/ibm_security_logout";
        idpLogoutURL = "https://" + getIdpHostName() + ":" + getIdpHostSecurePort() + "/idp/Logout";
        localLogoutOnly = false;

        idpUserName = SAMLConstants.IDP_USER_NAME;
        idpUserPwd = SAMLConstants.IDP_USER_PWD;
        spRegUserName = SAMLConstants.SP_USER_NAME;
        spRegUserPwd = SAMLConstants.SP_USER_PWD;
        useIdAssertion = true;

        // chc - needed with Shibboleth relayState = null;
        relayState = httpsStart + SAMLConstants.APP1;
        accessTokenType = SAMLConstants.SP_ACCESS_TOKEN_TYPE;
        spCookieName = null;
        includeTokenInSubject = false;
        isEncryptedAssertion = false;

        samlTokenValidationData = new SAMLTokenValidationData();
        printSAMLTokenValidationData();

    }

    /**
     * Using the currently chosen TFIM server, return the IDP challenge provider requested
     *
     * @param provider
     *                     - index of requested provider
     * @return
     * @throws Exception
     */
    /* default challenge uses http */
    //	public String setSpecificIDPChallenge(int provider) throws Exception {
    public void setSpecificIDPChallenge(int provider) throws Exception {
        setSpecificIDPChallenge(provider, false);
    }

    public void setSpecificIDPChallenge(int provider, Boolean secure) throws Exception {

        //String federation = SAMLConstants.IDP_FEDERATION_LISTS[provider-1][getServerIndex()] ;

        if (cttools.isIDPADFS(getIdpRoot())) {

            //FIXME this is not the right federation as it contains sp1 sp2 or defaultSP but the relaying party name is required in ADFS so it shows the login page
            String federation = SAMLConstants.IDP_FEDERATION_LISTS[provider - 1][getServerIndex()];
            setIdpChallenge("https://" + getSelectedIDPServerName() + ":" + getSelectedIDPServerSecurePort() + "/adfs/ls/IdpInitiatedSignOn.aspx");
            setIdpIssuer("http://" + getSelectedIDPServerName() + "/adfs/services/trust");

        } else {
            String federation = SAMLConstants.IDP_FEDERATION_LISTS[provider - 1][getServerIndex()];
            if (secure) {
                setIdpChallenge("https://" + getIdpHostName() + ":" + getIdpHostSecurePort() + "/idp/profile/SAML2/Unsolicited/SSO");
            } else {
                setIdpChallenge("http://" + getIdpHostName() + ":" + getIdpHostPort() + "/idp/profile/SAML2/Unsolicited/SSO");
                setIdpIssuer("http://" + getIdpHostName() + ":" + getIdpHostPort() + "/idp/shibboleth");
            }
            setIdpIssuer("https://" + getIdpHostName() + ":" + getIdpHostSecurePort() + "/idp/shibboleth");
        }

        samlTokenValidationData = new SAMLTokenValidationData();
    }

    /**
     * Return the hostname of the selected TFIM server
     *
     * @return
     * @throws Exception
     */
    public String getSelectedIDPServerName() throws Exception {

        return SAMLConstants.IDP_SERVER_LIST[getServerIndex()].split(":")[0];
    }

    /**
     * Return the hostname of the selected TFIM server
     *
     * @return
     * @throws Exception
     */
    public String getSelectedIDPServerPort() throws Exception {

        return SAMLConstants.IDP_SERVER_LIST[getServerIndex()].split(":")[1];
    }

    /**
     * Return the hostname of the selected TFIM server
     *
     * @return
     * @throws Exception
     */
    public String getSelectedIDPServerSecurePort() throws Exception {

        return SAMLConstants.IDP_SERVER_LIST[getServerIndex()].split(":")[2];
    }

    public String[] selectIDPServer() throws Exception {
        return selectIDPServer(SAMLConstants.SHIBBOLETH_TYPE);
        //      return selectIDPServer(SAMLConstants.BOTH_IDP_TYPE);
        //		return selectIDPServer(SAMLConstants.ADFS_TYPE) ;
        //		return selectIDPServer(SAMLConstants.TFIM_TYPE) ;
    }

    public String[] selectIDPServer(String idpType) throws Exception {

        Log.info(thisClass, "selectIDPServer", "IDP Server type is: " + idpType);

        String[] filteredIDPServerList = filterIDPServerList(SAMLConstants.IDP_SERVER_LIST, idpType);
        chosen_IDP_server = null;
        while ((chosen_IDP_server == null) && (filteredIDPServerList != null)) {
            chosen_IDP_server = testHelpers.getServerFromList(filteredIDPServerList);
            Log.info(thisClass, "selectIDPServer", "checking server: " + chosen_IDP_server[0]);
            //if login page accessable - we've found a good derver
            if (isIDPServerWorking(chosen_IDP_server)) {
                break;
            } else {
                // server's IDP function is NOT working - skip it
                filteredIDPServerList = removeChosenFromFilteredIDPServerList(filteredIDPServerList, chosen_IDP_server);
                chosen_IDP_server = null;
            }
        }

        if (chosen_IDP_server == null) {
            throw new Exception("No TFIM server appears to be active - Test class is terminating");
        }

        Log.info(thisClass, "selectTFIMIDPServer", "Using Server: " + chosen_IDP_server[0]);
        setServerIndex(determineServerIndex(chosen_IDP_server));
        Log.info(thisClass, "selectTFIMIDPServer", getSelectedIDPServerName());
        setIdpRoot(getShortName(getSelectedIDPServerName()));

        return chosen_IDP_server;
    }

    public String[] selectIDPServer(SAMLTestServer idpServer) throws Exception {

        chosen_IDP_server = new String[3];
        chosen_IDP_server[0] = "localhost";
        chosen_IDP_server[1] = idpServer.getHttpDefaultPort().toString();
        chosen_IDP_server[2] = idpServer.getHttpDefaultSecurePort().toString();

        Log.info(thisClass, "selectTFIMIDPServer", "Using Server: " + chosen_IDP_server[0]);
        setServerIndex(determineServerIndex(chosen_IDP_server));
        Log.info(thisClass, "selectTFIMIDPServer", getSelectedIDPServerName());
        setIdpRoot(getShortName(getSelectedIDPServerName()));
        setIdpHostName(chosen_IDP_server[0]);
        setIdpHostPort(chosen_IDP_server[1]);
        setIdpHostSecurePort(chosen_IDP_server[2]);

        return chosen_IDP_server;
    }

    public String getShortName(String longName) {

        if (longName == null) {
            return longName;
        } else {
            return longName.split("\\.")[0].split("\\:")[0];
        }

    }

    public String[] filterIDPServerList(String[] origList, String idpType) throws Exception {

        // TODO - may need to expand on this for shibboleth
        if (idpType.equals(SAMLConstants.BOTH_IDP_TYPE)) {
            return origList;
        } else {
            int numServers = origList.length;
            if (numServers == 0) {
                throw new Exception("None of the servers in the list appear to be active - Test class is terminating");
            }
            ArrayList<String> newServerList = new ArrayList<String>();
            for (int i = 0; i < numServers; i++) {
                String shortName = getShortName(origList[i]);
                Log.info(thisClass, "filterIDPServerList", "shortName: " + shortName);
                if (idpType.equals(SAMLConstants.TFIM_TYPE)) {
                    if (cttools.isIDPTFIM(shortName)) {
                        newServerList.add(origList[i]);
                    }
                } else {
                    if (idpType.equals(SAMLConstants.ADFS_TYPE)) {
                        if (cttools.isIDPADFS(shortName)) {
                            newServerList.add(origList[i]);
                        }
                    } else {
                        if (idpType.equals(SAMLConstants.SHIBBOLETH_TYPE)) {
                            if (cttools.isIDPSHIBBOLETH(shortName)) {
                                newServerList.add(origList[i]);
                            }
                        }
                    }
                }

            }
            return newServerList.toArray(new String[newServerList.size()]);
        }
    }

    public String getDefaultDetailIdpEndPoint() throws Exception {

        return "http://" + getSelectedIDPServerName() + ":" + getSelectedIDPServerPort() + SAMLConstants.FEDERATION_JSP;

    }

    private void setDefaultOIDCServerTestSettings(String httpStart, String httpsStart) throws Exception {
        // should only be setting values specific to the OIDC server
    }

    public String[] getChosenTFIMServer() {
        return chosen_IDP_server;
    }

    public void setIdpEndPoint(String inIdpEndPoint) {
        idpEndPoint = inIdpEndPoint;
    }

    public String getIdpEndPoint() {
        return idpEndPoint;
    }

    public void setIdpChallenge(String inIdpChallenge) {
        idpChallenge = inIdpChallenge;
    }

    public String getIdpChallenge() {
        return idpChallenge;
    }

    public void setSpConsumer(String inSpConsumer) {
        spConsumer = inSpConsumer;
    }

    public String getSpConsumer() {
        return spConsumer;
    }

    public void setSpTargetApp(String inSpTargetApp) {
        spTargetApp = inSpTargetApp;
    }

    public String getSpTargetApp() {
        return spTargetApp;
    }

    public void setSpDefaultApp(String inSpDefaultApp) {
        spDefaultApp = inSpDefaultApp;
    }

    public String getSpDefaultApp() {
        return spDefaultApp;
    }

    public void setSpAlternateApp(String inSpAlternateApp) {
        spAlternateApp = inSpAlternateApp;
    }

    public String getSpAlternateApp() {
        return spAlternateApp;
    }

    public void setSpMetaDataEdpt(String inSpMetaDataEdpt) {
        spMetaDataEdpt = inSpMetaDataEdpt;
    }

    public String getSpMetaDataEdpt() {
        return spMetaDataEdpt;
    }

    public String getIdpUserName() {
        return idpUserName;
    }

    public void setIdpUserName(String inIdpUserName) {
        idpUserName = inIdpUserName;
    }

    public String getIdpUserPwd() {
        return idpUserPwd;
    }

    public void setIdpUserPwd(String inIdpUserPwd) {
        idpUserPwd = inIdpUserPwd;
    }

    public String getSpRegUserName() {
        return spRegUserName;
    }

    public void setSpRegUserName(String inSpRegUserName) {
        spRegUserName = inSpRegUserName;
    }

    public String getSpRegUserPwd() {
        return spRegUserPwd;
    }

    public void setSpRegUserPwd(String inSpRegUserPwd) {
        spRegUserPwd = inSpRegUserPwd;
    }

    public String getIdpIssuer() {
        return idpIssuer;
    }

    public void setIdpIssuer(String inIdpIssuer) {
        idpIssuer = inIdpIssuer;
    }

    public int getServerIndex() {
        return serverIndex;
    }

    public void setServerIndex(int inServerIndex) {
        serverIndex = inServerIndex;
    }

    public String getIdpRoot() {
        return idpRoot;
    }

    public void setIdpRoot(String inIdpRoot) {
        idpRoot = inIdpRoot;
    }

    public Boolean getUseIdAssertion() {
        return useIdAssertion;
    }

    public void setUseIdAssertion(Boolean inUseIdAssertion) {
        useIdAssertion = inUseIdAssertion;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String inRelayState) {
        relayState = inRelayState;
    }

    public String getAccessTokenType() {
        return accessTokenType;
    }

    public void setAccessTokenType(String inAccessTokenType) {
        accessTokenType = inAccessTokenType;
    }

    public String getSpCookieName() {
        return spCookieName;
    }

    public void setSpCookieName(String inSpCookieName) {
        spCookieName = inSpCookieName;
    }

    public Boolean getIncludeTokenInSubject() {
        return includeTokenInSubject;
    }

    public void setIncludeTokenInSubject(Boolean inIncludeTokenInSubject) {
        includeTokenInSubject = inIncludeTokenInSubject;
    }

    public String getIdpHostName() {
        return idpHostName;
    }

    public void setIdpHostName(String inIdpHostName) {
        idpHostName = inIdpHostName;
    }

    public String getIdpHostPort() {
        return idpHostPort;
    }

    public void setIdpHostPort(String inIdpHostPort) {
        idpHostPort = inIdpHostPort;
    }

    public String getIdpHostSecurePort() {
        return idpHostSecurePort;
    }

    public void setIdpHostSecurePort(String inIdpHostSecurePort) {
        idpHostSecurePort = inIdpHostSecurePort;
    }

    public ArrayList<UpdateTimeVars> getSamlTokenUpdateTimeVars() {
        return copySamlUpdateTimeVars(samlTokenUpdateTimeVars);
    }

    public void setSamlTokenUpdateTimeVars(ArrayList<UpdateTimeVars> presetList) {
        samlTokenUpdateTimeVars = copySamlUpdateTimeVars(presetList);
    }

    public void setSamlTokenUpdateTimeVars(String attribute, Boolean addTime, int[] time, Boolean useCurrentTime) {
        if (samlTokenUpdateTimeVars == null) {
            samlTokenUpdateTimeVars = new ArrayList<UpdateTimeVars>();
        }
        UpdateTimeVars newVars = new UpdateTimeVars();
        newVars.attribute = attribute;
        newVars.addTime = addTime;
        newVars.time = time.clone();
        newVars.useCurrentTime = useCurrentTime;
        samlTokenUpdateTimeVars.add(newVars);
    }

    public ArrayList<ReplaceVars> getSamlTokenReplaceVars() {
        return copySamlReplaceVars(samlTokenReplaceVars);
    }

    public void setSamlTokenReplaceVars(ArrayList<ReplaceVars> presetList) {
        samlTokenReplaceVars = copySamlReplaceVars(presetList);
    }

    public void setSamlTokenReplaceVars(String oldVar, String newVar, String newLoc) {
        if (samlTokenReplaceVars == null) {
            samlTokenReplaceVars = new ArrayList<ReplaceVars>();
        }
        ReplaceVars newVars = new ReplaceVars();
        newVars.oldValue = oldVar;
        newVars.newValue = newVar;
        newVars.location = newLoc;
        samlTokenReplaceVars.add(newVars);
    }

    public SAMLTokenValidationData getSamlTokenValidationData() {
        return samlTokenValidationData;
    }

    public void setSamlTokenValidationData(String inNameId, String inIssuer, String inInResponseTo, String inMessageID, String inEncryptionKeyUser, String inRecipient,
                                           String inEncryptAlg) {
        samlTokenValidationData = new SAMLTokenValidationData(inNameId, inIssuer, inInResponseTo, inMessageID, inEncryptionKeyUser, inRecipient, inEncryptAlg);
    }

    public int getTokenReuseSleep() {
        return tokenReuseSleep;
    }

    public void setTokenReuseSleep(int inTokenReuseSleep) {
        tokenReuseSleep = inTokenReuseSleep;
    }

    public int getSleepBeforeTokenUse() {
        return sleepBeforeTokenUse;
    }

    public void setSleepBeforeTokenUse(int inSleepBeforeTokenUse) {
        sleepBeforeTokenUse = inSleepBeforeTokenUse;
    }

    public void setSpLogoutURL(String inSpLogoutURL) {
        spLogoutURL = inSpLogoutURL;
    }

    public String getSpLogoutURL() {
        return spLogoutURL;
    }

    public void setIdpLogoutURL(String inIdpLogoutURL) {
        idpLogoutURL = inIdpLogoutURL;
    }

    public String getIdpLogoutURL() {
        return idpLogoutURL;
    }

    public void setLocalLogoutOnly(Boolean inLocalLogoutOnly) {
        localLogoutOnly = inLocalLogoutOnly;
    }

    public Boolean getLocalLogoutOnly() {
        return localLogoutOnly;
    }

    public Boolean getIsEncryptedAssertion() {
        return isEncryptedAssertion;
    }

    public void setIsEncryptedAssertion(Boolean inIsEncryptedAssertion) {
        isEncryptedAssertion = inIsEncryptedAssertion;
    }

    public CXFSettings getCXFSettings() {
        return cxfSettings;
    }

    public void setCXFSettings(String inTestMethod, String inClientType, String inPortNumber, String inSecurePort, String inId, String inPw, String inServiceName,
                               String inServicePort, String inSendMsg, String inReplayTest, String inManagedClient, String inClientWSDLFile) {
        cxfSettings = new CXFSettings(inTestMethod, inClientType, inPortNumber, inSecurePort, inId, inPw, inServiceName, inServicePort, inSendMsg, inReplayTest, inManagedClient, inClientWSDLFile);
    }

    public RSSettings getRSSettings() {
        return rsSettings;
    }

    public void setRSSettings(String inHeaderName, String inHeaderFormat, String inSamlTokenFormat) {
        rsSettings = new RSSettings(inHeaderName, inHeaderFormat, inSamlTokenFormat);
    }

    public void setRSSettings(RSSettings inRSSettings) {
        rsSettings = inRSSettings;
    }

    public void setRSSettings() {
        rsSettings = new RSSettings();
    }

    public RSSettings overWriteRSSettings(RSSettings orig, String inHeaderName, String inHeaderFormat, String inSamlTokenFormat) {

        String headerName = "saml_token";
        String headerFormat = SAMLConstants.SAML_HEADER_4;
        String samlTokenFormat = SAMLConstants.ASSERTION_ENCODED;
        if (orig != null) {
            if (orig.getHeaderName() != null) {
                headerName = orig.getHeaderName();
            }
            headerFormat = orig.getHeaderFormat();
            samlTokenFormat = orig.getSamlTokenFormat();
        }
        if (inHeaderName != null) {
            headerName = inHeaderName;
        }
        if (inHeaderFormat != null) {
            headerFormat = inHeaderFormat;
        }
        if (inSamlTokenFormat != null) {
            samlTokenFormat = inSamlTokenFormat;
        }

        Log.info(thisClass, "overWriteRSSettings", "Will set: " + headerName + " " + samlTokenFormat);
        return new RSSettings(headerName, headerFormat, samlTokenFormat);
    }

    // if new testSettings are added that contain the federation name, please include
    // an update for them in this method
    public void updateFederationInSettings(int federationNameIndex, Boolean isSecure) throws Exception {
        setSpecificIDPChallenge(federationNameIndex, isSecure);
        // set any other fields - if/when other test settings include the federation/provider

    }

    // if new testSettings are added that contain the Partner/SP name, please include
    // an update for them in this method
    // testSettings is created using the value of "sp"
    public void updatePartnerInSettings(String partner, Boolean isSecure) throws Exception {
        updatePartnerInSettings(null, partner, isSecure);
    }

    public void updatePartnerInSettings(String origPartner, String partner, Boolean isSecure) throws Exception {
        // set any other fields - if/when other test settings include the federation/provider
        setSpConsumer(replaceSettingIfNotNull(getSpConsumer(), origPartner, partner));
        setSpTargetApp(replaceSettingIfNotNull(getSpTargetApp(), origPartner, partner));
        setSpDefaultApp(replaceSettingIfNotNull(getSpDefaultApp(), origPartner, partner));
        setSpAlternateApp(replaceSettingIfNotNull(getSpAlternateApp(), origPartner, partner));
        setSpMetaDataEdpt(replaceSettingIfNotNull(getSpMetaDataEdpt(), origPartner, partner));
        setRelayState(replaceSettingIfNotNull(getRelayState(), origPartner, partner));
        setSpLogoutURL(replaceSettingIfNotNull(getSpLogoutURL(), origPartner, partner));
        setIdpLogoutURL(replaceSettingIfNotNull(getIdpLogoutURL(), origPartner, partner));

    }

    public String replaceSettingIfNotNull(String origString, String origPartner, String partner) throws Exception {
        Log.info(thisClass, "replaceSettingIfNotNull", "Before replace: " + origString);
        String orig = null;
        if (origPartner != null) {
            orig = origPartner;
        } else {
            orig = "defaultSP";
        }
        String newString = null;
        if (origString != null) {
            Pattern p = Pattern.compile("/" + orig + "$");
            Matcher m = p.matcher(origString);
            String newStringChange1 = m.replaceAll("/" + partner);
            String newStringChange2 = newStringChange1.replace("/" + orig + "/", "/" + partner + "/");
            newString = newStringChange2.replace("_" + orig + "/", "_" + partner + "/");

        }
        Log.info(thisClass, "replaceSettingIfNotNull", "After replace: " + newString);
        return newString;
    }

    public ArrayList<ReplaceVars> copySamlReplaceVars(ArrayList<ReplaceVars> oldList) {

        if (oldList != null) {
            ArrayList<ReplaceVars> newList = new ArrayList<ReplaceVars>();
            for (ReplaceVars oldEntry : oldList) {
                if (oldEntry != null) {
                    ReplaceVars newEntry = new ReplaceVars();
                    newEntry.oldValue = oldEntry.oldValue;
                    newEntry.newValue = oldEntry.newValue;
                    newEntry.location = oldEntry.location;
                    newList.add(newEntry);
                }
            }
            return newList;
        } else {
            return null;
        }
    }

    public String printSamlReplaceVars() {

        if (samlTokenReplaceVars != null) {
            String msgString = "";
            for (ReplaceVars theVars : samlTokenReplaceVars) {
                if (theVars != null) {
                    msgString = msgString + "[oldValue: " + theVars.oldValue + " newValue: " + theVars.newValue + " location: " + theVars.location + "] ";
                }
            }
            return msgString;
        } else {
            return null;
        }
    }

    public ArrayList<UpdateTimeVars> copySamlUpdateTimeVars(ArrayList<UpdateTimeVars> oldList) {

        if (oldList != null) {
            ArrayList<UpdateTimeVars> newList = new ArrayList<UpdateTimeVars>();
            for (UpdateTimeVars oldEntry : oldList) {
                if (oldEntry != null) {
                    UpdateTimeVars newEntry = new UpdateTimeVars();
                    newEntry.attribute = oldEntry.attribute;
                    newEntry.addTime = oldEntry.addTime;
                    newEntry.time = oldEntry.time.clone();
                    newEntry.useCurrentTime = oldEntry.useCurrentTime;
                    newList.add(newEntry);
                }
            }
            return newList;
        } else {
            return null;
        }
    }

    public String printSamlUpdateTimeVars() {

        if (samlTokenUpdateTimeVars != null) {
            String msgString = "";
            for (UpdateTimeVars theVars : samlTokenUpdateTimeVars) {
                if (theVars != null) {
                    msgString = msgString + "[attribute: " + theVars.attribute + " addTime: " + theVars.addTime + " time: " + printTimeAsString(theVars.time) + " useCurrentTime: "
                                + theVars.useCurrentTime.toString() + "] ";
                }
            }
            return msgString;
        } else {
            return null;
        }
    }

    public String printTimeAsString(int[] time) {

        return "Time:  " + time[0] + " day(s), " + time[1] + " hour(s), " + time[2] + " minute(s) " + time[3] + " second(s)";
    }

    public static int[] setTimeArray(int day, int hour, int min, int second) {

        int[] newTime = { day, hour, min, second };
        return newTime;
    }

    public SAMLTokenValidationData copySamlTokeValidationData(SAMLTokenValidationData inData) {
        if (inData != null) {
            return new SAMLTokenValidationData(inData.getNameId(), inData.getIssuer(), inData.getInResponseTo(), inData.getMessageID(), inData.getEncryptionKeyUser(), inData
                            .getRecipient(), inData.getEncryptAlg());
        }
        return null;
    }

    public void printSAMLTokenValidationData() {

        if (samlTokenValidationData == null) {
            Log.info(thisClass, "printSAMLTokenValidationData", "samlTokenValidationData: null");
        } else {
            samlTokenValidationData.printSAMLTokenValidationData();
        }
    }

    public void setRemoveTagInResponse(String removeTagInResponse) {
        this.removeTagInResponse = removeTagInResponse;
    }

    public void setRemoveTagInResponse(String removeTagInResponse, String removeTagInResponse2) {
        this.removeTagInResponse = removeTagInResponse;
        this.removeTagInResponse2 = removeTagInResponse2;
    }

    public String getRemoveTagInResponse() {
        return removeTagInResponse;
    }

    public String getRemoveTagInResponse2() {
        return removeTagInResponse2;
    }

    public void setRemoveAttribAll(String removeAttribAll) {
        this.removeAttribAll = removeAttribAll;
    }

    public String getRemoveAttribAll() {
        return removeAttribAll;
    }

    public CXFSettings copyCXFSettings(CXFSettings inCXFSettings) {
        if (inCXFSettings != null) {
            return new CXFSettings(inCXFSettings.getTestMethod(), inCXFSettings.getClientType(), inCXFSettings.getPortNumber(), inCXFSettings.getSecurePort(), inCXFSettings
                            .getId(), inCXFSettings.getPw(), inCXFSettings.getServiceName(), inCXFSettings.getServicePort(), inCXFSettings
                                            .getSendMsg(), inCXFSettings.getReplayTest(), inCXFSettings.getManagedClient(), inCXFSettings.getClientWSDLFile());
        }
        return null;
    }

    public void printCXFSettings() {

        if (cxfSettings == null) {
            Log.info(thisClass, "printCXFSettings", "cxfSettings: null");
        } else {
            cxfSettings.printCXFSettings();
        }
    }

    public RSSettings copyRSSettings(RSSettings inRSSettings) {
        if (inRSSettings != null) {
            return new RSSettings(inRSSettings.getHeaderName(), inRSSettings.getHeaderFormat(), inRSSettings.getSamlTokenFormat());
        }
        return null;
    }

    public void printRSSettings() {

        if (rsSettings == null) {
            Log.info(thisClass, "printRSSettings", "rsSettings: null");
        } else {
            rsSettings.printRSSettings();
        }
    }

    public Boolean isIDPServerWorking(String[] chosen_IDP_server) throws Exception {

        // TODO - temp hack
        return true;

        //        String thisMethod = "isIDPServerWorking";
        //        msgUtils.printMethodName(thisMethod);
        //
        //        WebConversation wc = new WebConversation();
        //
        //        WebRequest request = null;
        //        String theTitle = null;
        //        if (cttools.isIDPADFS(getShortName(chosen_IDP_server[0]))) {
        //            request = new GetMethodWebRequest("https://" + chosen_IDP_server[0] + ":" + chosen_IDP_server[2] + "/adfs/ls/IdpInitiatedSignOn.aspx");
        //            theTitle = SAMLConstants.SAML_ADFS_LOGIN_PROMPT_PAGE;
        //        } else {
        //            if (cttools.isIDPSHIBBOLETH(getShortName(chosen_IDP_server[0]))) {
        //                request = new PostMethodWebRequest("https://" + chosen_IDP_server[0] + ":" + chosen_IDP_server[2] + "/idp/profile/SAML2/POST/SSO");
        //                theTitle = SAMLConstants.SAML_SHIBBOLETH_LOGIN_PROMPT_PAGE;
        //            } else {
        //                String federation = SAMLConstants.IDP_FEDERATION_LISTS[0][determineServerIndex(chosen_IDP_server)];
        //                request = new GetMethodWebRequest("https://" + chosen_IDP_server[0] + ":" + chosen_IDP_server[2] + "/sps/" + federation + "/saml20/logininitial");
        //                request.setParameter("PartnerId", "https://localhost:8020/ibm/saml20/defaultSP");
        //                theTitle = SAMLConstants.SAML_TFIM_LOGIN_HEADER;
        //            }
        //        }
        //
        //        Log.info(thisClass, thisMethod, "Returned request is: " + request.toString());
        //
        //        String returnedTitle = null;
        //        WebResponse response = null;
        //        try {
        //            // make the connection time out after 30 seconds so the check doesn't hang
        //            Log.info(thisClass, thisMethod, "system Read timeout: " + System.getProperty("sun.net.client.defaultReadTimeout"));
        //            Log.info(thisClass, thisMethod, "system Connection timeout: " + System.getProperty("sun.net.client.defaultConnectTimeout"));
        //            System.setProperty("sun.net.client.defaultReadTimeout", "300000");
        //            System.setProperty("sun.net.client.defaultConnectTimeout", "300000");
        //            response = wc.getResponse(request);
        //
        //            returnedTitle = response.getTitle();
        //            //System.setProperty("sun.net.client.defaultReadTimeout", "300000");
        //            //System.setProperty("sun.net.client.defaultConnectTimeout", "300000");
        //
        //        } catch (Exception e) {
        //            Log.error(thisClass, thisMethod, e);
        //            returnedTitle = null;
        //        }
        //        Log.info(thisClass, thisMethod, "Requested Title: " + theTitle);
        //        Log.info(thisClass, thisMethod, "Ping of IDP server returned the following Title: " + returnedTitle);
        //        if (returnedTitle != null && returnedTitle.contains(theTitle)) {
        //            return true;
        //        } else {
        //            msgUtils.printResponseParts(response, "testSetup", "Server doesn't seem to be active response from Login page is: ");
        ////TODO  chc          return false;
        //            return true;
        //        }
    }

    public int determineServerIndex(String[] chosen_IDP_server) throws Exception {
        for (int i = 0; i < SAMLConstants.IDP_SERVER_LIST.length; i++) {
            Log.info(thisClass, "determineServerIndex", "TFIM_SERVER: " + SAMLConstants.IDP_SERVER_LIST[i]);
            if ((SAMLConstants.IDP_SERVER_LIST[i].split(":")[0].equals(chosen_IDP_server[0]))) {
                //                &&
                //                (SAMLConstants.IDP_SERVER_LIST[i].split(":")[1].equals(chosen_IDP_server[1])) &&
                //                (SAMLConstants.IDP_SERVER_LIST[i].split(":")[2].equals(chosen_IDP_server[2]))
                return i;
            }
        }
        throw new Exception("");
    }

    public String[] removeChosenFromFilteredIDPServerList(String[] origList, String[] chosen_IDP_server) throws Exception {

        ArrayList<String> newServerList = new ArrayList<String>();

        int numServers = origList.length;
        if (numServers == 0) {
            throw new Exception("None of the servers in the list appear to be active - Test class is terminating");
        }

        for (int i = 0; i < numServers; i++) {
            //        	String shortName = getShortName(origList[i]) ;

            Log.info(thisClass, "removeChosenFromFilteredIDPServerList", "Orig list: " + origList[i]);
            Log.info(thisClass, "removeChosenFromFilteredIDPServerList", "Chosen Server" + chosen_IDP_server[0]);
            if (!origList[i].startsWith(chosen_IDP_server[0])) {
                newServerList.add(origList[i]);
            }

        }
        return newServerList.toArray(new String[newServerList.size()]);
    }
}
