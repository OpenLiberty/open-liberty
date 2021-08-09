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
package com.ibm.ws.wssecurity.cxf.interceptor;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.ext.WSSecurityException.ErrorCode;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.Timestamp;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.opensaml.saml.saml2.core.Assertion;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.wssecurity.WSSecurityPolicyException;
import com.ibm.ws.wssecurity.caller.CallerConstants;
import com.ibm.ws.wssecurity.caller.SAMLAuthenticator;
import com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.token.TokenUtils;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public class WSSecurityLibertyCallerInterceptor extends AbstractSoapInterceptor {
    private static final TraceComponent tc = Tr.register(WSSecurityLibertyCallerInterceptor.class, WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    protected static final String multiple_unt_exist_err = "More than one Username token is found in the message, " +
                                                           "cannot identify caller candidate.";
    protected static final String no_unt_exist_err = "There is no Username token in the message to process caller.";
    protected static final String multiple_saml_exist_err = "More than one Saml token is found in the message, " +
                                                            "cannot identify caller candidate.";
    protected static final String no_saml_exist_err = "There is no Saml token in the message to process caller.";
    protected static final String no_x509_token_exist_err = "There is no X509 token in the message to process caller.";
    protected static final String unknown_caller_token_name = "Caller token name specified is not valid.";
    protected static final String empty_results_list = "Empty results list";
    protected static final String error_authenticate = "Cannot authenticate caller token";
    protected static final String no_asymmetric_token = "There is no Asymmetric signature " +
                                                        "token exists in the message";
    protected static final String multiple_asymmetric_token_err = "Multiple Asymmetric signature tokens in the message, cannot identify caller";
    protected static final String internal_err = "Security service is not available.";

    public static final String KEY_SSO_SERVICE = "ssoService";
    protected static final ConcurrentServiceReferenceMap<String, SsoService> ssoServiceRefs =
                    new ConcurrentServiceReferenceMap<String, SsoService>(KEY_SSO_SERVICE);

    public WSSecurityLibertyCallerInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        //getAfter().add(object)

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(@Sensitive SoapMessage message) throws Fault {
        if (message == null) {
            return;
        }
        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        if (isOut) {
            //outbound on server side or outbound on 
            //client side doesn't need any caller stuff

            return;
        } else {
            //client inbound doesn't need any caller stuff
            if (isReq) {
                return;
            }
        }
        // if there is no caller config specified then we return from here.
        Map<String, Object> callerConfigMap = (Map<String, Object>) message.getContextualProperty(WSSecurityConstants.CALLER_CONFIG);
        String callerName = null;
        if (callerConfigMap != null && !callerConfigMap.isEmpty()) {
            callerName = (String) callerConfigMap.get(WSSecurityConstants.CALLER_NAME);
        }

        if (callerName == null || callerName.isEmpty()) {
            return;
        }

        boolean isUNT = false;
        boolean isX509 = false;
        boolean isSaml = false;

        if (WSSecurityConstants.UNT_CALLER_NAME.equalsIgnoreCase(callerName)) {
            isUNT = true;
        } else if (WSSecurityConstants.X509_CALLER_NAME.equalsIgnoreCase(callerName)) {
            isX509 = true;
        } else if (WSSecurityConstants.SAML_CALLER_NAME.equalsIgnoreCase(callerName)) {
            isSaml = true;
        } else {
            //Tr.error(tc, "UNKNOWN_CALLER_TOKEN_NAME", new Object[] { callerName });
            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
                                              (WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, /* unknown_caller_token_name */
                                              "invalidTokenType", new Object[]
                                              { callerName }));
            throw fault;

        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " caller config found = ", callerName);
        }

        if (message.get(WSHandlerConstants.RECV_RESULTS) != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, " results found");
            }

            List<WSHandlerResult> wsResult =
                            (List<WSHandlerResult>) message.get(WSHandlerConstants.RECV_RESULTS);
            WSHandlerResult handlerResult = wsResult.get(0);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, " ws result = " + handlerResult.getResults());
                Tr.debug(tc, " ws action result = " + handlerResult.getActionResults());
            }
            if (isUNT) {
                handleUsernameToken(message, handlerResult);
            } else if (isX509) {
                handleX509Token(message, handlerResult);
            } else if (isSaml) {
                handleSamlToken(message, handlerResult, callerConfigMap);
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, " NO RESULTS!!!");
            }
            //Tr.error(tc, "empty_results_list");
            Tr.error(tc, "no_caller_exist_err", new Object[] { callerName, callerName });
            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "unhandledToken", new Object[]
                                              { callerName }));
            throw fault;
        }

    }

    /**
     * @param message
     * @param handlerResult
     * @param callerConfigMap
     */
    private void handleSamlToken(SoapMessage message, WSHandlerResult handlerResult, Map<String, Object> callerConfigMap) {
        List<WSSecurityEngineResult> samlResults = new ArrayList<WSSecurityEngineResult>();
        //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.ST_SIGNED, samlResults);
        samlResults = handlerResult.getActionResults().get(WSConstants.ST_SIGNED);

        if (samlResults.isEmpty()) {
            //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.ST_UNSIGNED, samlResults);
            samlResults = handlerResult.getActionResults().get(WSConstants.ST_UNSIGNED);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " saml caller token results = ", samlResults.size());
        }
        Principal principal = null;
        int cnt = 0;

        SamlAssertionWrapper assertion = null;
        for (WSSecurityEngineResult result : samlResults) {
            assertion = (SamlAssertionWrapper) result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
            if (assertion != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "assertion from the results =   ", assertion.getId());
                }
                //break;    
            }
            principal = (Principal) result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
            if (principal != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "principal =   ", principal);
                    Tr.debug(tc, "principal name =   ", principal.getName());
                }
                cnt++;
            }
        }

        if (cnt > 1) {
            //Error - more than one saml token
            Tr.error(tc, "multiple_saml_exist_err");
            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "duplicateError"));
            throw fault;

        } else if (cnt == 0) {
            //Error - no unt in the message to process caller configuration
            Tr.error(tc, "no_caller_exist_err", new Object[] { WSSecurityConstants.SAML_CALLER_NAME,
                                                              WSSecurityConstants.SAML_CALLER_NAME });
            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "unhandledToken", new Object[]
                                              { "SamlToken" }));
            throw fault;
        }

        Saml20Token samlToken = null;
        try {
            samlToken = handleSamlAssertion(assertion);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "token is created successfully =   ", samlToken.getSamlID());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            String msg = null;
            if (e.getCause() != null) {
                msg = e.getCause().getLocalizedMessage();
            }
            else {
                msg = e.getLocalizedMessage();
            }
            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badSamlToken", new Object[]
                                              { msg }));
            throw fault;
        }

        //login
        SAMLAuthenticator callerAuthenticator = new SAMLAuthenticator(callerConfigMap, samlToken);
        boolean isProcessed = false;
        try {
            AuthenticationResult result = callerAuthenticator.authenticate();
            if (result.getStatus() == AuthResult.SUCCESS) {
                Subject authenticatedSubject = result.getSubject();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, authenticated subject = ", authenticatedSubject);
                    Tr.debug(tc, "Authentication successful, runAsSubject before = ", WSSubject.getRunAsSubject());
                }

                WSSubject.setRunAsSubject(authenticatedSubject);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, runAsSubject after = ", WSSubject.getRunAsSubject());
                }

            }
            else {
                Exception ex;
                if ("User".equalsIgnoreCase((String) callerConfigMap.get(CallerConstants.MAP_TO_UR))) {
                    ex = new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badSamlToken", new Object[]
                    { "invalid user ID" });
                }
                else {
                    ex = new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badSamlToken", new Object[]
                    { result.getReason() });
                }

                isProcessed = true;
                throw ex;
            }
        } catch (Exception e1) {
            SoapFault fault;
            if (isProcessed) {
                fault = createSoapFault(message.getVersion(), (WSSecurityException) e1);
            }
            else {
                Tr.error(tc, "error_authenticate", new Object[] { e1.getMessage() });
                fault = createSoapFault(message.getVersion(), new WSSecurityException
                                        (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badSamlToken", new Object[]
                                        { e1.getLocalizedMessage() }));
            }
            throw fault;
        }

    }

    /**
     * @param ssoService
     * @param assertion
     * @throws Exception
     */
    private Saml20Token handleSamlAssertion(SamlAssertionWrapper assertionWrapper) throws Exception {
        Assertion assertion = assertionWrapper.getSaml2();
        Saml20Token token = null;
        try {
            token = TokenUtils.createSamlTokenFromAssertion(assertion);
        } catch (Exception e) {
            throw e;
        }
        return token;

    }

    private void handleUsernameToken(@Sensitive SoapMessage message, WSHandlerResult handlerResult) throws SoapFault {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "results = ", handlerResult);
        }
        List<WSSecurityEngineResult> utResults = new ArrayList<WSSecurityEngineResult>();
        //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.UT, utResults);
        if (handlerResult.getActionResults().containsKey(WSConstants.UT)) {
            utResults = handlerResult.getActionResults().get(WSConstants.UT);
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "UNT results = ", utResults);
        }
        // Check for Caller UNT w/o password @sw1
        //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.UT_NOPASSWORD, utResults);
        List<WSSecurityEngineResult> utnpResults = new ArrayList<WSSecurityEngineResult>();
        utnpResults = handlerResult.getActionResults().get(WSConstants.UT_NOPASSWORD);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "UNT_NP results = ", utnpResults);
        }
        if (utnpResults != null) {
            utResults.addAll(utnpResults);
        }
        
        int ut_counter = 0;
        WSUsernameTokenPrincipalImpl principal = null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, " Number of UNT results = ", utResults.size());
        }
        for (WSSecurityEngineResult result : utResults) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, " UNT result = ", result);
            }
            principal = (WSUsernameTokenPrincipalImpl) result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
            if (principal != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, " principal =   ", principal);
                    Tr.debug(tc, " principal name =   ", principal.getName());
                }

                ut_counter++;

            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, " user name token principal is NULL!!! ");

                }
            }

        }//for utResults
        if (ut_counter > 1) {
            //Error - more than one UNT
            Tr.error(tc, "multiple_unt_exist_err");
            throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                             (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "duplicateError"));
            //
            //SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
            //                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "duplicateError"));
            //throw fault;

        } else if (ut_counter == 0) {
            //Error - no unt in the message to process caller configuration
            Tr.error(tc, "no_caller_exist_err", new Object[] { WSSecurityConstants.UNT_CALLER_NAME,
                                                              WSSecurityConstants.UNT_CALLER_NAME });
            throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                             (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "missingUsernameToken"));
            //SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
            //                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "missingUsernameToken"));
            //throw fault;
        }
        //login
        SecurityService securityService = UsernameTokenValidator.getSecurityService();
        if (securityService != null) {
            AuthenticationService authenticationService = securityService.getAuthenticationService();
            Subject tempSubject = new Subject();

            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            if (!authenticationService.isAllowHashTableLoginWithIdOnly())
                hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            hashtable.put(/* AttributeNameConstants.WSCREDENTIAL_USERID */
                          "com.ibm.wsspi.security.cred.userId", principal.getName());
            tempSubject.getPublicCredentials().add(hashtable);
            try {
                Subject new_subject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tempSubject);
                //authResult = new AuthenticationResult(AuthResult.SUCCESS, new_subject);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, authenticated subject = ", new_subject);
                    Tr.debug(tc, "Authentication successful, runAsSubject before = ", WSSubject.getRunAsSubject());
                }

                WSSubject.setRunAsSubject(new_subject);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, runAsSubject after = ", WSSubject.getRunAsSubject());
                }

            } catch (AuthenticationException e) {
                FFDCFilter.processException(e,
                                            getClass().getName(), "handleMessage",
                                            new Object[] { principal.getName() });
                Tr.error(tc, "error_authenticate", new Object[] { e.getMessage() });
                //authResult = new AuthenticationResult(AuthResult.SEND_401, e.getMessage());
                
                throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                                 (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[] {e.getLocalizedMessage()}));
                //
//                SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[]
//                                                  { e.getLocalizedMessage() }));
//                throw fault;
            } catch (com.ibm.websphere.security.WSSecurityException wse) {
                //e.printStackTrace();
                FFDCFilter.processException(wse,
                                            getClass().getName(), "handleMessage",
                                            new Object[] { principal.getName() });
                Tr.error(tc, "error_authenticate", new Object[] { wse.getMessage() });
                throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                                 (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[] {wse.getLocalizedMessage()}));
//                SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[]
//                                                  { wse.getLocalizedMessage() }));
//                throw fault;
            } catch (Exception e) {
                //e.printStackTrace();
                FFDCFilter.processException(e,
                                            getClass().getName(), "handleMessage",
                                            new Object[] { principal.getName() });
                Tr.error(tc, "error_authenticate", new Object[] { e.getMessage() });
                throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                                 (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[] {e.getMessage()}));
//                SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[]
//                                                  { e.getMessage() }));
//                throw fault;
            }
        } else {
            throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException
                                             (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[] {"Missing Liberty Security Service"}));
//            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "badUsernameToken", new Object[]
//                                              { "Missing Liberty Security Service" }));
//            throw fault;
        }
    }

    private void handleX509Token(@Sensitive SoapMessage message, WSHandlerResult handlerResult) throws SoapFault {

        //org.apache.ws.security.message.token.BinarySecurity binarySecurityToken = null;
        java.security.cert.X509Certificate[] x509Certs = null;

        boolean isAsymmetricBinding = false;
        boolean isEndorsingSupportingTokenBinding = false;
        boolean isSignedEndorsingSupportingTokenBinding = false;
        boolean isEndorsingEncryptedSupportingTokenBinding = false;
        boolean isSignedEndorsingEncryptedSupportingTokenBinding = false;

        List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
        //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.SIGN, signedResults);
        if (handlerResult.getActionResults().containsKey(WSConstants.SIGN)) {
            signedResults = handlerResult.getActionResults().get(WSConstants.SIGN);
        }
              
        //WSSecurityUtil.fetchAllActionResults(handlerResult.getResults(), WSConstants.UT_SIGN, signedResults);
        List<WSSecurityEngineResult> signedutResults = handlerResult.getActionResults().get(WSConstants.UT_SIGN);
        if (signedutResults != null) {
            signedResults.addAll(signedutResults);
        }     
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais != null && !ais.isEmpty()) {
            isAsymmetricBinding = true;
        }

        ais = aim.get(SP12Constants.ENDORSING_SUPPORTING_TOKENS);
        if (ais != null && !ais.isEmpty()) {
            isEndorsingSupportingTokenBinding = true;
        }
        ais = aim.get(SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        if (ais != null && !ais.isEmpty()) {
            isSignedEndorsingSupportingTokenBinding = true;
        }
        ais = aim.get(SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null && !ais.isEmpty()) {
            isEndorsingEncryptedSupportingTokenBinding = true;
        }
        ais = aim.get(SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (ais != null && !ais.isEmpty()) {
            isSignedEndorsingEncryptedSupportingTokenBinding = true;
        }
        if (isAsymmetricBinding) {
            //look at all the signedResults
            x509Certs = getClientX509(message, handlerResult.getResults(), signedResults);

        }
        //If we don't have one asymmetric signature token result by now, then look at all endorsing supporting tokens
        else if (isEndorsingSupportingTokenBinding ||
                 isSignedEndorsingSupportingTokenBinding ||
                 isEndorsingEncryptedSupportingTokenBinding ||
                 isSignedEndorsingEncryptedSupportingTokenBinding) {
            //look at all token results
            x509Certs = getEndorsingX509(message, handlerResult.getResults(), signedResults);

        }

        if (x509Certs == null) {
            // no caller token error 
            //Tr.error(tc, "no_endorsing_token_no_asymmetric_token");
            Tr.error(tc, "no_caller_exist_err", new Object[] { WSSecurityConstants.X509_CALLER_NAME,
                                                              WSSecurityConstants.X509_CALLER_NAME });
            throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidCertData", new Object[]
                            { "0" }));
//            SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidCertData", new Object[]
//                                              { "0" }));
//            throw fault;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Caller DN: " + x509Certs[0].getSubjectDN().getName());
        }
        bstCertAuthentication(x509Certs, message, message.getVersion());

    } //else if (isX509)

    private X509Certificate[] getClientX509(@Sensitive SoapMessage message, List<WSSecurityEngineResult> results,
                                            List<WSSecurityEngineResult> signedResults) throws SoapFault {
        //look at all the signedResults

        String issuerInfo = null;
        X509Certificate[] x509Certs = null;
        //boolean matchToken = false;
        //make sure that all the signature results have same cert. 
        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate cert =
                            (X509Certificate) signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
            //cert = cert.getSerialNumber();
            if (cert != null) {
                StringBuffer sb = new StringBuffer(cert.getSerialNumber().toString());
                sb.append(cert.getIssuerDN().getName());
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "issuer sn and dn = ", sb.toString());
                }
                if (issuerInfo == null || sb.toString().equals(issuerInfo)) {
                    x509Certs = (X509Certificate[]) signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
                    issuerInfo = sb.toString();
                    continue;
                } else if (!(sb.toString().equals(issuerInfo))) {
                    Tr.error(tc, "multiple_asymmetric_token_err");
                    throw WSS4JUtils.createSoapFault(message, message.getVersion(), new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidCertData", new Object[]
                                    { "2" }));
//                    SoapFault fault = createSoapFault(message.getVersion(), new WSSecurityException
//                                                      (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidCertData", new Object[]
//                                                      { "2" }));
//                    throw fault;
                }
            }
        }
        return x509Certs;
    }

    private X509Certificate[] getEndorsingX509(@Sensitive SoapMessage message, List<WSSecurityEngineResult> results,
                                               List<WSSecurityEngineResult> signedResults) {

        X509Certificate[] x509Certs = null;

        if (isTransportBinding(message)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "looking x509Token which endorse TS");
            }
            WSSecurityEngineResult tsResult = fetchActionResult(results, WSConstants.TS);//WSSecurityUtil.fetchActionResult(results, WSConstants.TS); //v3
            Element timestamp = null;
            if (tsResult != null) {
                Timestamp ts = (Timestamp) tsResult.get(WSSecurityEngineResult.TAG_TIMESTAMP);
                timestamp = ts.getElement();
                x509Certs = getEndorsingX509(timestamp, signedResults);
            }
        } else {
            x509Certs = getEndorsingX509(signedResults);
        }
        return x509Certs;
    }
    
    public static WSSecurityEngineResult fetchActionResult(List<WSSecurityEngineResult> resultList, int action) {

        WSSecurityEngineResult returnResult = null;

        for (WSSecurityEngineResult result : resultList) {
            //
            // Check the result of every action whether it matches the given action
            //
            int resultAction = ((java.lang.Integer) result.get(WSSecurityEngineResult.TAG_ACTION)).intValue();
            if (resultAction == action) {
                returnResult = result;
            }
        }

        return returnResult;
    }

    private X509Certificate[] getEndorsingX509(List<WSSecurityEngineResult> signedResults) {

        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate[] x509Cert = (X509Certificate[]) signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
            if (x509Cert != null) {
                List<WSDataRef> sl =
                                CastUtils.cast((List<?>) signedResult.get(
                                                WSSecurityEngineResult.TAG_DATA_REF_URIS
                                                ));
                if (sl != null && sl.size() == 1) {
                    for (WSDataRef dataRef : sl) {
                        QName signedQName = dataRef.getName();
                        if (WSConstants.SIGNATURE.equals(signedQName)) { //2020
                            return x509Cert;
                        }
                    }
                }
            }
        }
        return null;
    }

    private X509Certificate[] getEndorsingX509(Element timestamp,
                                               List<WSSecurityEngineResult> signedResults) {

        for (WSSecurityEngineResult signedResult : signedResults) {
            X509Certificate[] x509Certs = (X509Certificate[]) signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATES);
            if (x509Certs != null) {
                List<WSDataRef> sl =
                                CastUtils.cast((List<?>) signedResult.get(
                                                WSSecurityEngineResult.TAG_DATA_REF_URIS
                                                ));
                if (sl != null) {
                    for (WSDataRef dataRef : sl) {
                        if (timestamp == dataRef.getProtectedElement()) {
                            return x509Certs;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void bstCertAuthentication(X509Certificate[] x509Certs, @Sensitive SoapMessage message, SoapVersion version) throws Fault {
        //login
        SecurityService securityService = UsernameTokenValidator.getSecurityService();
        if (securityService != null) {
            AuthenticationService authenticationService = securityService.getAuthenticationService();

            try {
                String thisAuthMech = JaasLoginConfigConstants.SYSTEM_WEB_INBOUND;
                AuthenticationData authenticationData = new WSAuthenticationData();

                authenticationData.set(AuthenticationData.CERTCHAIN, x509Certs);
                Subject authenticatedSubject = authenticationService.authenticate(thisAuthMech, authenticationData, null);
                //authResult = new AuthenticationResult(AuthResult.SUCCESS, authenticatedSubject);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, authenticated subject = ", authenticatedSubject);
                    Tr.debug(tc, "Authentication successful, runAsSubject before = ", WSSubject.getRunAsSubject());
                }

                WSSubject.setRunAsSubject(authenticatedSubject);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Authentication successful, runAsSubject after = ", WSSubject.getRunAsSubject());
                }
            } catch (AuthenticationException e) {
                //authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
                FFDCFilter.processException(e,
                                            getClass().getName(), "bstCertAuthentication",
                                            new Object[] { x509Certs[0].getSubjectX500Principal().getName() });
                //authResult = new AuthenticationResult(AuthResult.SEND_401, e.getMessage());
                Tr.error(tc, "error_authenticate", new Object[] { e.getMessage() });
                throw WSS4JUtils.createSoapFault(message, version, new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
                                { e.getLocalizedMessage() }));
//                SoapFault fault = createSoapFault(version, new WSSecurityException
//                                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
//                                                  { e.getLocalizedMessage() }));
//                throw fault;
            } catch (Exception e) {
                //e.printStackTrace();
                FFDCFilter.processException(e,
                                            getClass().getName(), "handleMessage",
                                            new Object[] { x509Certs[0].getSubjectX500Principal().getName() });
                Tr.error(tc, "error_authenticate", new Object[] { e.getMessage() });
                throw WSS4JUtils.createSoapFault(message, version, new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
                                { e.getMessage() }));
//                SoapFault fault = createSoapFault(version, new WSSecurityException
//                                                  (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
//                                                  { e.getMessage() }));
//                throw fault;
            }
        } else {
            throw WSS4JUtils.createSoapFault(message, version, new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
                            { "Missing Liberty Security Service" }));
//            SoapFault fault = createSoapFault(version, new WSSecurityException
//                                              (WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, "invalidData", new Object[]
//                                              { "Missing Liberty Security Service" }));
//            throw fault;
        }
    }

    private boolean isTransportBinding(@Sensitive SoapMessage message) {
        // See whether TLS is in use or not
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        boolean isTransportBinding = false;
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais != null && !ais.isEmpty()) {
            isTransportBinding = true;
        }

        return isTransportBinding;
    }

    private SoapFault createSoapFault(SoapVersion version, WSSecurityException e) {
        SoapFault fault;
        javax.xml.namespace.QName faultCode = e.getFaultCode();
        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(e.getMessage(), e, faultCode);
        } else {
            fault = new SoapFault(e.getMessage(), e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }
}
