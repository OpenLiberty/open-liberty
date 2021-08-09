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
package com.ibm.ws.security.saml20.fat.commonTest;

import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

public class SAMLCommonValidationTools {

    private final Class<?> thisClass = SAMLCommonValidationTools.class;
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static SAMLCommonTestTools cttools = new SAMLCommonTestTools();

    SAMLTestServer testSAMLServer = null;
    SAMLTestServer testSAMLOIDCServer = null;
    SAMLTestServer testOIDCServer = null;
    SAMLTestServer testAppServer = null;
    SAMLTestServer testIdpServer = null;
    String[] validLogLocations = { SAMLConstants.CONSOLE_LOG, SAMLConstants.SAML_CONSOLE_LOG, SAMLConstants.OIDC_CONSOLE_LOG, SAMLConstants.SAMLOIDC_CONSOLE_LOG, SAMLConstants.APP_CONSOLE_LOG,
            SAMLConstants.MESSAGES_LOG, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.OIDC_MESSAGES_LOG, SAMLConstants.SAMLOIDC_MESSAGES_LOG, SAMLConstants.APP_MESSAGES_LOG,
            SAMLConstants.TRACE_LOG, SAMLConstants.SAML_TRACE_LOG, SAMLConstants.OIDC_TRACE_LOG, SAMLConstants.SAMLOIDC_TRACE_LOG, SAMLConstants.APP_TRACE_LOG, SAMLConstants.IDP_PROCESS_LOG };
    public static final String HEADER_DELIMITER = "|";

    public void setServers(SAMLTestServer theSAML, SAMLTestServer theSAMLOIDC, SAMLTestServer theOIDC, SAMLTestServer theApp, SAMLTestServer theIdp) throws Exception {

        testSAMLServer = theSAML;
        testSAMLOIDCServer = theSAMLOIDC;
        testOIDCServer = theOIDC;
        testAppServer = theApp;
        testIdpServer = theIdp;
    }

    /**
     * Validates the results of a step in the test process
     *
     * @param wc
     *            TODO
     * @param response
     *            - the response output from the latest step in the test process
     * @param currentAction
     *            - the latest test step/action performed
     * @param expectations
     *            - an array of validationData - these include the string to
     *            search for, how to do the search, where to search and for
     *            which test stop/action do expect the string
     *
     * @throws exception
     */
    public void validateResult(WebConversation wc, WebResponse response,
            String currentAction, List<validationData> expectations, SAMLTestSettings settings)
            throws Exception {

    }

    //    public void validateResult(Object response,
    //            String currentAction, List<validationData> expectations, SAMLTestSettings settings)
    //            throws Exception {
    //        WebClient webClient = ((HtmlPage)response).getWebClient() ;
    //        validateResult(webClient, response, currentAction, expectations, settings) ;
    //    }

    public void validateResult(WebClient webClient, Object response,
            String currentAction, List<validationData> expectations, SAMLTestSettings settings)
            throws Exception {

        String thisMethod = "validateResult";
        msgUtils.printMethodName(thisMethod, "Start of");

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);

        try {
            // we get passed in the response from the form submissions as well
            // as a list of string pairs - these pairs contain strings
            // that we search for in the response as well as the
            // corresponding error message that will be
            // issued in a failed assertion
            if (response == null) {
                Log.info(thisClass, thisMethod, "Response is null");
                return;
            }
            if (expectations == null) {
                Log.info(thisClass, thisMethod, "expectations is null");
                return;
            }
            Log.info(thisClass, "getResponseStatusCode", "Response is of type: " + response.getClass().getSimpleName());
            for (validationData expected : expectations) {

                if (currentAction.equals(expected.getAction())) {
                    if ((expected.getWhere().equals(SAMLConstants.RESPONSE_FULL)
                            || expected.getWhere().equals(SAMLConstants.RESPONSE_TITLE)
                            || expected.getWhere().equals(SAMLConstants.RESPONSE_STATUS)
                            || expected.getWhere().equals(SAMLConstants.RESPONSE_URL)
                            || expected.getWhere().equals(SAMLConstants.RESPONSE_HEADER)
                            || expected.getWhere().equals(SAMLConstants.RESPONSE_MESSAGE))) {
                        validateWithResponse(response, expected);
                    } else {
                        if (cttools.isInList(validLogLocations, expected.getWhere())) {
                            validateWithServerLog(expected);
                        } else {
                            if (expected.getWhere().equals(SAMLConstants.RESPONSE_TOKEN)) {
                                validateReponseLTPAToken(response, expected);
                            } else {
                                if (expected.getWhere().equals(SAMLConstants.SAML_TOKEN)) {
                                    validateSAMLTokenContent(response, expected, settings, SAMLConstants.STRING_EQUALS);
                                } else {
                                    if (expected.getWhere().equals(SAMLConstants.SAML_TOKEN_ENCRYPTED)) {
                                        validateSAMLTokenContent(response, expected, settings, SAMLConstants.STRING_CONTAINS, true);
                                    } else {
                                        if (expected.getWhere().equals(SAMLConstants.SAML_TOKEN_CONTAINS)) {
                                            validateSAMLTokenContent(response, expected, settings, SAMLConstants.STRING_CONTAINS);
                                        } else {
                                            if (expected.getWhere().equals(SAMLConstants.SAML_POST_TOKEN)) {
                                                validateSAMLPostTokenContent(response, expected, settings);
                                            } else {
                                                if (expected.getWhere().equals(SAMLConstants.SAML_METADATA)) {
                                                    validateSAMLMetaDataContent(response, expected, settings);
                                                } else {
                                                    if (expected.getWhere().equals(SAMLConstants.EXCEPTION_MESSAGE)) {
                                                        Log.info(thisClass, thisMethod, "Excpetion validated separately");
                                                    } else {
                                                        if (expected.getWhere().equals(SAMLConstants.COOKIES)) {
                                                            validateConversationCookies(webClient, expected);
                                                        } else {
                                                            Log.info(thisClass, thisMethod, "Unknown validation type: " + expected.getWhere());
                                                            throw new Exception("Unknown validation type");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            msgUtils.printMethodName(thisMethod, "End of");
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    /***
     * invoke the correct server instance to validate content in it's log
     */
    public void validateWithServerLog(validationData expected) throws Exception {

        Log.info(thisClass, "validateWithServerLog", "passed in Log name is: " + expected.getWhere());
        if (expected.getWhere().contains(SAMLConstants.SAML_MESSAGES_LOG) || expected.getWhere().contains(SAMLConstants.SAML_TRACE_LOG)) {
            testSAMLServer.validateWithServerLog(expected.getCheckType(), expected.getWhere(), expected.getPrintMsg(), expected.getValidationValue());
        } else {
            if (expected.getWhere().contains(SAMLConstants.OIDC_MESSAGES_LOG)) {
                testSAMLOIDCServer.validateWithServerLog(expected.getCheckType(), expected.getWhere(), expected.getPrintMsg(), expected.getValidationValue());
            } else {
                if (expected.getWhere().contains("generic")) {
                    testOIDCServer.validateWithServerLog(expected.getCheckType(), expected.getWhere(), expected.getPrintMsg(), expected.getValidationValue());
                } else {
                    if (expected.getWhere().contains(SAMLConstants.APP_MESSAGES_LOG)) {
                        testAppServer.validateWithServerLog(expected.getCheckType(), expected.getWhere(), expected.getPrintMsg(), expected.getValidationValue());
                    } else {
                        if (expected.getWhere().contains(SAMLConstants.IDP_PROCESS_LOG)) {
                            testIdpServer.validateWithServerLog(expected.getCheckType(), expected.getWhere(), expected.getPrintMsg(), expected.getValidationValue());
                        } else {
                            throw new Exception("Unrecognized server specified");
                        }
                    }
                }
            }

        }

    }

    /**
     * Searches for a message string in the response
     *
     * @param reponse
     *            - the response output to search through
     * @param expected
     *            - a validationMsg type to search (contains the string to
     *            search for)
     * @throws exception
     */
    public void validateWithResponse(Object response,
            validationData expected) throws Exception {

        String thisMethod = "validateWithResponse";
        msgUtils.printMethodName(thisMethod, "Start of");

        String responseContent = null;

        try {
            if (expected.getWhere().equals(SAMLConstants.RESPONSE_FULL)) {
                responseContent = AutomationTools.getResponseText(response);
            } else {
                if (expected.getWhere().equals(SAMLConstants.RESPONSE_TITLE)) {
                    responseContent = AutomationTools.getResponseTitle(response);
                } else {
                    if (expected.getWhere().equals(SAMLConstants.RESPONSE_MESSAGE)) {
                        responseContent = AutomationTools.getResponseMessage(response);
                    } else {
                        if (expected.getWhere().equals(SAMLConstants.RESPONSE_URL)) {
                            responseContent = AutomationTools.getResponseUrl(response);
                        } else {
                            if (expected.getWhere().equals(SAMLConstants.RESPONSE_HEADER)) {
                                String[] hs = AutomationTools.getResponseHeaderNames(response);
                                StringBuilder sb = new StringBuilder();
                                for (String h : hs) {
                                    sb.append(h + ": " + AutomationTools.getResponseHeaderField(response, h));
                                    sb.append(HEADER_DELIMITER);
                                }
                                responseContent = sb.toString();
                            } else {
                                if (expected.getWhere().equals(SAMLConstants.RESPONSE_STATUS)) {
                                    // if we have a status code that results in an exception, that is handled differently
                                    //                                    if (cttools.isInList(generatesExceptionList, expected.validationValue)) {
                                    //                                        return;
                                    //                                    } else {
                                    responseContent = Integer.toString(AutomationTools.getResponseStatusCode(response));
                                    //                                    }
                                } else {
                                    Log.info(thisClass, thisMethod,
                                            "No valid WebResponse area specified - assuming ALL");
                                    responseContent = AutomationTools.getResponseText(response);
                                }
                            }
                        }
                    }
                }
            }

            validateResponseContent(response, responseContent, expected);

            Log.info(thisClass, thisMethod, "Checked Value: " + expected.getValidationValue());
            return;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }

    }

    /**
     * Validates the provided response content against the expected values.
     */
    public void validateResponseContent(Object response, String responseContent, validationData expected) throws Exception {
        String method = "validateResponseContent";

        Log.info(thisClass, method, "Response section is: " + expected.getWhere() + "    ||    " + "checkType is: " + expected.getCheckType());
        Log.info(thisClass, method, "Checking for: " + expected.getValidationValue());
        if (responseContent == null) {
            Log.info(thisClass, method, "Response content was null");
            if (expected.getValidationValue() != null) {
                fail(expected.getWhere() + ": " + expected.getPrintMsg()
                        + "\n Was expecting [" + expected.getValidationValue() + "] but received: " + responseContent);
            }
            return;
        }

        String fullResponseContent = AutomationTools.getFullResponseContentForFailureMessage(response, expected.getWhere());

        if (expected.getCheckType().equals(SAMLConstants.STRING_CONTAINS)) {
            msgUtils.assertTrueAndLog(method, expected.getWhere() + ": " + expected.getPrintMsg()
                    + "\n Was expecting [" + expected.getValidationValue() + "] but received: " + responseContent + "." + fullResponseContent,
                    responseContent.contains(expected.getValidationValue()));
            return;
        }
        if (expected.getCheckType().equals(SAMLConstants.STRING_DOES_NOT_CONTAIN)) {
            msgUtils.assertTrueAndLog(method, expected.getWhere() + ": " + expected.getPrintMsg()
                    + "\n Was expecting NOT TO FIND [" + expected.getValidationValue() + "] but received: " + responseContent + "." + fullResponseContent,
                    !responseContent.contains(expected.getValidationValue()));
            return;
        }
        if (expected.getCheckType().equals(SAMLConstants.STRING_MATCHES)) {
            Pattern p = Pattern.compile(expected.getValidationValue());
            Matcher m = p.matcher(responseContent);

            msgUtils.assertTrueAndLog(method, expected.getWhere() + ": " + expected.getPrintMsg()
                    + " Was expecting [" + expected.getValidationValue() + "] but received: " + responseContent + "." + fullResponseContent,
                    m.find());
            return;
        }

        throw new Exception("String comparison type unknown - test case coded incorrectly");
    }

    /**
     * This method verifies expected strings/attributes in the SAMLResponse by grabbing
     * a specified line from a specified log file, finding the pattern: value="insert
     * encoded SAMLResponse," and decoding it.
     *
     * The method will return true when expectations are met, else returns false. The method returns
     * true when the expected string is found in the specified log and isExpectedInLog is true.
     * If the expected string is not found in the specified log and isExpectedInLog is true,
     * the method returns false. If isExpectedInLog is false and the expected string is found
     * in the specified log, the method returns false. Otherwise if isExpectedInLog is false and
     * the expected string is not found in the log, the method returns true.
     *
     * @param stringToFind
     *            - unique value of line within log to pull SAMLResponse from in the logs
     * @param expectedString
     *            - the attributes and corresponding values of the attribute checked for
     * @param isExpectedInLog
     *            - whether you expect the string/attribute to be there or not
     * @param logFile
     *            - which log to pull SAMLResponse from
     * @param testName
     *            - testName to trace log messages in output.txt file
     * @param samlServer
     *            - the server of where the tests are running
     */
    public boolean verifyLogContains(String stringToFind, String expectedString, boolean isExpectedInLog, String logFile, String testName, SAMLTestServer samlServer) {
        String val = null;
        String decodedVal = null;
        String decoded = null;
        try {
            val = samlServer.searchValueInServerLog(stringToFind, logFile);
            Log.info(thisClass, testName, val);
            Pattern p = Pattern.compile("value=\"([^\"]*)\"");
            Matcher m = p.matcher(val);
            //loop through all patterns found until pattern matches that specified, p
            while (m.find()) {
                Log.info(thisClass, testName, "matching: " + m.group(1));
                try {
                    decoded = cttools.decodeSamlResponseString(m.group(1));
                    if (decoded != null) {
                        decodedVal = decoded;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.error(thisClass, testName, e, m.group(1) + " does not match the pattern: value=\"encodedSAMLResponse\"");
                    //					Do not break the process here as we need to verify if the rest of the matches contain valid saml responses
                    //					See defect 186813
                    //					return false;
                }
            }
            if (decodedVal != null) {
                boolean isStringInVal = decodedVal.contains(expectedString);
                if (isStringInVal && isExpectedInLog) {
                    //expected string found in log
                    Log.info(thisClass, testName, "found: " + expectedString);
                    return true;
                } else if (isStringInVal && !isExpectedInLog) {
                    Log.info(thisClass, testName, "The expected string: " + expectedString + ", was present but did not expect it to be in log");
                    return false;
                } else if (!isStringInVal && isExpectedInLog) {
                    Log.info(thisClass, testName, "The expected string: " + expectedString + ", was not present but expected it to be in log");
                    return false;
                } else if (!isStringInVal && !isExpectedInLog) {
                    Log.info(thisClass, testName, "The expected string: " + expectedString + ", was not present and did not expect it to be in log");
                    return true;
                }
            }
            Log.info(thisClass, testName, "decodedValue was null, unable to verify expectedString");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, testName, e, "the log " + logFile + " does not contain the string: " + stringToFind);
            return false;
        }
    }

    public void validateReponseLTPAToken(Object response, validationData expected) throws Exception {

        //TODO
        //        String thisMethod = "validateReponseLTPAToken";
        //        msgUtils.printMethodName(thisMethod, "Start of");
        //
        //        try {
        //
        //            boolean hasLtpaToken = false;
        //            String[] cookies = response.getNewCookieNames();
        //            if (cookies != null) {
        //                for (String cookie : cookies) {
        //                    if (cookie.equals(SAMLConstants.LTPA_TOKEN)) {
        //                        Log.info(thisClass, thisMethod, "Cookie content: " + cookie);
        //                        hasLtpaToken = true;
        //                        break;
        //                    }
        //                }
        //            }
        //            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(),
        //                    Boolean.valueOf(expected.getValidationValue()) == hasLtpaToken);
        //
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //            Log.error(thisClass, thisMethod, e, "Error validating response");
        //            throw e;
        //        }
    }

    public void validateException(List<validationData> expectations, String action, Exception e) throws Exception {

        String thisMethod = "validateException";

        msgUtils.printMethodName(thisMethod, "Start of");

        //        try {
        // we get passed in the response from the form submissions as well
        // as a list of string pairs - these pairs contain strings
        // that we search for in the response as well as the
        // corresponding error message that will be
        // issued in a failed assertion
        if (e == null || e.getMessage() == null) {
            Log.info(thisClass, thisMethod, "Exception is null");
            throw e;
        }
        Log.info(thisClass, thisMethod, "exception content: " + e.getMessage());
        if (expectations == null) {
            Log.info(thisClass, thisMethod, "expectations is null");
            throw e;
        }
        Boolean found = false;
        for (validationData expected : expectations) {

            if (action.equals(expected.getAction())) {
                if (expected.getWhere().equals(SAMLConstants.EXCEPTION_MESSAGE)) {
                    msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg() + " received: " + e,
                            ((e.getMessage().contains(expected.getValidationValue())) || (e.toString().contains(expected.getValidationValue()))));
                    found = true;
                }
            }
        }
        msgUtils.printMethodName(thisMethod, "End of");
        if (found) {
            return;
        } else {
            Log.info(thisClass, thisMethod, "Action that hit the exception did NOT have an expectation defined to validate an exception");
            throw e;
        }

        //        } catch (Exception n) {
        //            e.printStackTrace();
        //            Log.error(thisClass, thisMethod, n, "Error validating response");
        //            throw n;
        //        }

    }

    //    public void validateDataLengthInResponse(WebResponse response, validationData expected) throws Exception {
    //
    //    	String thisMethod = "validateDataLengthInResponse";
    //
    //        msgUtils.printMethodName(thisMethod, "Start of");
    //        String key = expected.getValidationKey();
    //        String value = null;
    //        if (key.contains(" ")) {
    //        	value = getValueFromResponseFull(response, expected.getValidationKey());
    //        } else {
    //        	value = getTokenFromResponse(response, expected.getValidationKey());
    //        }
    //        if (value.length() != 30 ) {
    //            msgUtils.assertTrueAndLog(thisMethod, "The length (" + value.length() + ") of the specified key: " + expected.getValidationKey() + " is not " + expected.getValidationValue(), value.length() == Integer.parseInt(expected.getValidationValue()));
    //        }
    //
    //        msgUtils.printMethodName(thisMethod, "End of");
    //    }
    //
    //    public String removeQuote(String str){
    //
    //    	if (str == null || str == ""){
    //    		return null;
    //    	}
    //
    //    	char char1 = str.charAt(0);
    //    	char char2 = str.charAt(str.length() - 1);
    //    	if (char1 == '"' && char2 == '"')	{
    //    		str = str.substring(1, str.length() - 1);
    //    	}
    //    	return str;
    //    }
    //

    public void validateSAMLTokenContent(Object response, validationData expected, SAMLTestSettings settings, String checkType) throws Exception {
        validateSAMLTokenContent(response, expected, settings, checkType, false);
    }

    public void validateSAMLTokenContent(Object response, validationData expected, SAMLTestSettings settings, String checkType, boolean isAssertionEncrypted) throws Exception {

        String thisMethod = "validateSAMLTokenContent";
        msgUtils.printMethodName(thisMethod, "Start of");

        String encryptionCert = "MIIDBjCCAe6gAwIBAgIEVydUZTANBgkqhkiG9w0BAQsFADAvMQswCQYDVQQGEwJVUzEMMAoGA1UE";
        //"MIIDBjCCAe6gAwIBAgIEVydUZTANBgkqhkiG9w0BAQsFADAvMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMRIwEAYDVQQDDAluZXdfdXNlcjIwHhcNMTYwNTAyMTMyMTQxWhcNMzUwNzAxMTMyMTQxWjAvMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMRIwEAYDVQQDDAluZXdfdXNlcjIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCXrqxihzkdcYeeu0YIxnobzMsecCX7GE/ts5AyarASq3Aj7Ikkbdqy8uElCKLM/RwwDfXt6QkTpUAnYMFPyDaemolJcr8i5VCiQzgI2nQghsPrqqBUsngNLWPN/oYVyIkCIY4fO2XwgJ3v+rQS+5hzB3+xSNOvBGmQtwQfZn8UTayHhrzZTDLFmRfeCFtn/cwW2wyPPDRxKoo9SuAYKvhlwBZl6jJ7zVwvJtjlVv/n95l+R4EgPBJKyyolGaw0EWzbdz5EDKa0po/vCxDDkBMR4jul7SSJYumofjC2wNXI3UL+n69klKJrQ5An/Pz1vk6SakULqoYEbELt46TvXAInAgMBAAGjKjAoMBMGA1UdIwQMMAqACIwWUD/0aQzNMBEGA1UdDgQKBAiMFlA/9GkMzTANBgkqhkiG9w0BAQsFAAOCAQEAP6loJKkjdMul7lPmn3N+nWf0+kyXsDp8DeFCGrh2lGozBLnUobd3c/+edL+zZnhlmoE+7ByktdWcfgOS6aB8BGHfUlyPnEm4hd9AOA5dHh7up0gowCU1HeY5j2MY6ztQtK5aE6EZ5vOCrvqAS1sBBsRqQ+QJUxVMOnJpsbqjTuvOSxgiDY/XS7/8WYnehfz7cAlBQFdGSOIA9YJ2zjoK54J/yV6Fk9vtFdim6l3odryRuwIJ5f77ib8LmkB88VGC8zLizeVucj6W/QrDm6VktuCXTjAwJveo60mVMYfdAMTuT+9/WfJbr6xCgrGB8IYGi8JKlWgp+sgJYLY7hsU4Kw==" ;

        // maybe create a list of lists and pass all of them in at once
        try {
            ArrayList<String> location = null;

            // check the Issuer in the header
            location = new ArrayList<String>();
            location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
            location.add(SAMLConstants.SAML_TOKEN_ISSUER);
            Log.info(thisClass, thisMethod, "Checking the SAML token Issuer");
            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(),
                    cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getIssuer(), location, SAMLConstants.ELEMENT_TYPE, SAMLConstants.STRING_EQUALS));

            // check the nameID
            location = new ArrayList<String>();
            location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
            if (isAssertionEncrypted) {
                // When the assertion is encrypted, check for the appropriate key owner
                Log.info(thisClass, thisMethod, "Checking the SAML token encrypted assertion");
                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTED_ASSERTION);
                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTED_DATA);
                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_METHOD);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_KEY_INFO);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTED_KEY);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_KEY_INFO);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_X509);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_X509_CERT);
                //                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTION_KEY_NAME);
                msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(),
                        cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getEncryptAlg(), location, SAMLConstants.ALG_TYPE, checkType));
                //                cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getEncryptionKeyUser(), location, SAMLConstants.ELEMENT_TYPE, checkType));
            } else {
                //                Log.info(thisClass, thisMethod, "Checking the SAML token assertion, nameID, and subject");
                //                location.add(SAMLConstants.SAML_TOKEN_ASSERTION);
                //                location.add(SAMLConstants.SAML_TOKEN_SUBJECT);
                //                location.add(SAMLConstants.SAML_TOKEN_NAMEID);
                //                msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(),
                //                        cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getNameId(), location, SAMLConstants.ELEMENT_TYPE, SAMLConstants.STRING_CONTAINS));
            }

            //			if (settings.getSamlTokenValidationData().getInResponseTo() != null) {
            Log.info(thisClass, thisMethod, "Validating inReponseTo");
            location = new ArrayList<String>();
            location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
            if (isAssertionEncrypted) {
                location.add(SAMLConstants.SAML_TOKEN_ENCRYPTED_ASSERTION);
            } else {
                location.add(SAMLConstants.SAML_TOKEN_ASSERTION);
            }
            location.add(SAMLConstants.SAML_TOKEN_SUBJECT);
            location.add("saml2:SubjectConfirmation");
            location.add("saml2:SubjectConfirmationData");
            location.add("InResponseTo");
            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getInResponseTo(), location, SAMLConstants.NODE_EXISTS_TYPE, SAMLConstants.STRING_EQUALS));
            //		}
            if (settings.getSamlTokenValidationData().getRecipient() != null) {
                Log.info(thisClass, thisMethod, "Validating recipient");
                location = new ArrayList<String>();
                location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
                if (isAssertionEncrypted) {
                    location.add(SAMLConstants.SAML_TOKEN_ENCRYPTED_ASSERTION);
                } else {
                    location.add(SAMLConstants.SAML_TOKEN_ASSERTION);
                }
                location.add(SAMLConstants.SAML_TOKEN_SUBJECT);
                location.add("saml2:SubjectConfirmation");
                location.add("saml2:SubjectConfirmationData");
                location.add("Recipient");
                msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getRecipient(), location, SAMLConstants.NODE_EXISTS_TYPE, SAMLConstants.STRING_CONTAINS));

            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
    }

    public void validateSAMLPostTokenContent(Object response, validationData expected, SAMLTestSettings settings) throws Exception {

        String thisMethod = "validateSAMLPostTokenContent";
        msgUtils.printMethodName(thisMethod, "Start of");

        // maybe create a list of lists and pass all fo them in at once
        try {
            ArrayList<String> location = null;

            // check the Issuer in the header
            location = new ArrayList<String>();
            location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
            location.add(SAMLConstants.SAML_TOKEN_ISSUER);
            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getIssuer(), location, SAMLConstants.ELEMENT_TYPE, SAMLConstants.STRING_EQUALS));
            // check the nameID
            location = new ArrayList<String>();
            location.add(SAMLConstants.SAML_TOKEN_RESPONSE);
            location.add(SAMLConstants.SAML_TOKEN_STATUS);
            location.add(SAMLConstants.SAML_TOKEN_STATUS_CODE);

            //            location.add(SAMLConstants.SAML_TOKEN_STATUS_DETAIL);
            //            location.add(SAMLConstants.TFIM_TOKEN_STATUS_DETAIL);
            //            location.add(SAMLConstants.MESSAGE_ID);
            //            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), cttools.checkSAMLTokenValue(response, settings.getSamlTokenValidationData().getMessageID(), location, SAMLConstants.NODE_TYPE, SAMLConstants.STRING_EQUALS));
            //chc       msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), cttools.checkSAMLTokenValue(response, "Success", location, SAMLConstants.ELEMENT_TYPE, SAMLConstants.STRING_CONTAINS));
            // check the issuer in assertion

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
    }

    public void validateSAMLMetaDataContent(Object response, validationData expected, SAMLTestSettings settings) throws Exception {

        String thisMethod = "validateSAMLMetaDataContent";
        String xml = AutomationTools.getResponseText(response);
        System.out.println("RAW XML: " + xml);
        System.out.println("END RAW XML: ");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        String theValue = null;

        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            listNodes(doc.getDocumentElement(), ""); // Root element & children
            //    	    listNodes(doc.getFirstChild(),"");         // Root element & children
            //    	    listNodes(doc.getLastChild(),"");         // Root element & children

        } catch (Exception e) {
            Log.info(thisClass, thisMethod, "failure with doc builder");
            e.printStackTrace();
            throw e;
        }
    }

    static void listNodes(Node node, String indent) {
        String nodeName = node.getNodeName();
        String nodeValue = node.getNodeValue();
        System.out.println(indent + " Node: " + nodeName + " Value: " + nodeValue);
        short type = node.getNodeType();

        NodeList list = node.getChildNodes();
        if (list.getLength() > 0) {
            System.out.println(indent + " Child Nodes of " + nodeName + " are:");
            for (int i = 0; i < list.getLength(); i++) {
                listNodes(list.item(i), indent + "  ");
            }
            //    } else {
        }
        NamedNodeMap xx = node.getAttributes();
        if ((xx != null) && (xx.getLength() > 0)) {
            for (int i = 0; i < xx.getLength(); i++) {
                System.out.println(indent + " Attribute " + xx.item(i).getNodeName() + " value: " + xx.item(i).getNodeValue());

            }
        }

    }

    /***
     * Validate the cookies in the conversation
     * We may just want to make sure that we have a cookie that "starts with" a specific string, or we may be looking for a
     * specific
     * cookie name, or we may want a specific cookie name with a specific value
     *
     * @param webClient
     *            - the client/conversation
     * @param expected
     *            - the expectation to validate
     * @throws Exception
     */
    public void validateConversationCookies(WebClient webClient, validationData expected) throws Exception {

        String thisMethod = "validateConversationCookies";
        msgUtils.printMethodName(thisMethod, "Start of");

        Boolean found = false;
        msgUtils.printAllCookies(webClient);

        Set<Cookie> cookies = webClient.getCookieManager().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(expected.getValidationKey())) {
                if (expected.getValidationValue() != null) {
                    if (expected.getValidationValue().equals(cookie.getValue())) {
                        found = true;
                    }
                } else {
                    found = true;
                }
            }
        }

        if (expected.getCheckType().equals(SAMLConstants.STRING_CONTAINS)) {
            Log.info(thisClass, thisMethod, "Cookie: " + expected.getValidationKey() + " should be found");
            msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), found);
        } else {
            if (expected.getCheckType().equals(SAMLConstants.STRING_DOES_NOT_CONTAIN)) {
                Log.info(thisClass, thisMethod, "Cookie: " + expected.getValidationKey() + " should NOT be found");
                msgUtils.assertTrueAndLog(thisMethod, expected.getWhere() + ": " + expected.getPrintMsg(), !found);
            } else {
                Log.info(thisClass, thisMethod, "Test case passed an unrecognized checkType to validateConversationCookies");
                throw new Exception();
            }
        }

    }
}
